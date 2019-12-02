package com.farmerbb.notepad.login;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.enums.RuntimeABI;
import com.farmerbb.notepad.R;
import com.farmerbb.notepad.activity.BaseActivity;
import com.farmerbb.notepad.activity.MainActivity;
import com.farmerbb.notepad.activity.NotepadBaseActivity;
import com.farmerbb.notepad.loginface.Constants;
import com.farmerbb.notepad.loginface.RegisterAndRecognizeActivity;
import com.farmerbb.notepad.util.DataBase;



import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class Login extends NotepadBaseActivity{
    private Button login;
    private Button face;
    private String user;
    private String password;
    private TextView forgetPassword;
    private TextView register;
    private DataBase dbHelper;        //数据库
    private boolean find = false;

    public static final int LOGIN_OK = 1;

    private static final String TAG = "Login";
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    // 在线激活所需的权限
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //控件获取
        login = (Button) findViewById(R.id.login_login);
        forgetPassword = (TextView) findViewById(R.id.forgetPassword_login);
        register = (TextView) findViewById(R.id.register_login);
        face = (Button) findViewById(R.id.login_face);
        dbHelper = new DataBase(this, "UserStore.db", null, 1);
        //登入
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user = ((EditText) findViewById(R.id.user_login)).getText().toString().trim();
                password = ((EditText) findViewById(R.id.password_login)).getText().toString().trim();
                if (user.trim().length() < 11 && password.trim().length() < 6) {
                    Toast.makeText(Login.this, "请先输入正确的账户和密码", Toast.LENGTH_LONG).show();
                } else {
//                    调用数据库比对账户和密码
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    Cursor cursor = db.query("user", null, null, null, null, null, null);
                    if (cursor.moveToFirst()) {
                        do {
                            String user_phone = cursor.getString(cursor.getColumnIndex("user_phone"));
                            String user_password = cursor.getString(cursor.getColumnIndex("user_password"));
                            String head_image = cursor.getString(cursor.getColumnIndex("head_image"));
                            if (user.equals(user_phone) && password.equals(user_password)) {
                                cursor.close();
                                find = true;
                                //返回上一个activity
                                Intent intent = new Intent(Login.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } while (cursor.moveToNext());
                        cursor.close();
                    }
                    if (!find) {
                        Toast.makeText(Login.this, "账号或密码输入错误", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        //人脸注册&登陆
        face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //检测
                boolean allGranted = true;
                for (String neededPermission : NEEDED_PERMISSIONS) {
                    allGranted &= ContextCompat.checkSelfPermission(Login.this, neededPermission) == PackageManager.PERMISSION_GRANTED;
                }
                if (!allGranted) {
                    ActivityCompat.requestPermissions(Login.this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
                    return;
                }
                //激活
                Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) {
                        RuntimeABI runtimeABI = FaceEngine.getRuntimeABI();
                        Log.i(TAG, "subscribe: getRuntimeABI() " + runtimeABI);
                        int activeCode = FaceEngine.activeOnline(Login.this, Constants.APP_ID, Constants.SDK_KEY);
                        emitter.onNext(activeCode);
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            Log.e(TAG, "onNext: 激活成功" );
                            Toast.makeText(Login.this, "成功", Toast.LENGTH_SHORT).show();;
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            Log.e(TAG, "onNext: 已激活");
                        } else {
                            Log.e(TAG, "onNext: 失败" );
                            Toast.makeText(Login.this, "失败", Toast.LENGTH_SHORT).show();;
                        }

                        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
                        int res = FaceEngine.getActiveFileInfo(Login.this, activeFileInfo);
                        if (res == ErrorInfo.MOK) {
                            Log.i(TAG, activeFileInfo.toString());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
                //跳转到人脸注册界面
                checkLibraryAndJump(RegisterAndRecognizeActivity.class);
            }
        });
        //忘记密码
        forgetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转到忘记密码的界面
                Intent intent = new Intent(Login.this, ForgetActivity.class);
                startActivities(new Intent[]{intent});
            }
        });
        //注册账号
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转到注册账号界面
                Intent intent = new Intent(Login.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

    }

    void checkLibraryAndJump(Class activityClass) {
        startActivity(new Intent(this, activityClass));
    }
}