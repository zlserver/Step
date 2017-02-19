package zlcook.com.step;

import android.app.Activity;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.view.Window;

//启动界面
public class FirstActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        if (StepCounterService.FLAG || StepDetector.CURRENT_SETP > 0) {// 程序已经启动，直接跳转到运行界面
            Intent intent = new Intent(FirstActivity.this, MainActivity.class); //创建一个新的Intent，指定当前应用程序上下文
            startActivity(intent);
            this.finish();
        } else {
            //启动界面淡入淡出效果
            new CountDownTimer(2000L, 1000L)
            {
                public void onFinish()
                {
                    //启动界面淡入淡出效果
                    Intent intent = new Intent();
                    intent.setClass(FirstActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                }
                public void onTick(long paramLong)
                {}
            } .start();
        }

    }
}
