package zlcook.com.step;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    public static final String WEIGHT_VALUE = "weight_value";

    public static final String STEP_LENGTH_VALUE = "step_length_value";// 步长


    public static final String SETP_SHARED_PREFERENCES = "setp_shared_preferences";// 设置

    public static SharedPreferences sharedPreferences;

    private SharedPreferences.Editor editor;

    private TextView tv_step_length_vlaue;
    private TextView tv_weight_value;

    private SeekBar sb_step_length;
    private SeekBar sb_weight;

    private int step_length = 0;
    private int weight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.settings);
        /**
         * 初始化控件变量
         */
        addView();
        /**
         * 初始化设置信息：步长、体重
         */
        init();
        //监听滑动设置
        listener();

    }
    /**
     * 初始化控件变量
     */
    private void addView() {
        tv_step_length_vlaue = (TextView) this
                .findViewById(R.id.step_lenth_value);
        tv_weight_value = (TextView) this.findViewById(R.id.weight_value);

        sb_step_length = (SeekBar) this.findViewById(R.id.step_lenth);
        sb_weight = (SeekBar) this.findViewById(R.id.weight);

    }
    /**
     * 初始化设置信息：步长、体重
     */
    private void init() {
        // TODO Auto-generated method stub
        if (sharedPreferences == null) {    //SharedPreferences是Android平台上一个轻量级的存储类，
            //主要是保存一些常用的配置比如窗口状态
            sharedPreferences = getSharedPreferences(SETP_SHARED_PREFERENCES,
                    MODE_PRIVATE);
        }

        editor = sharedPreferences.edit();
        step_length = sharedPreferences.getInt(STEP_LENGTH_VALUE, 70);
        weight = sharedPreferences.getInt(WEIGHT_VALUE, 50);

        sb_step_length.setProgress((step_length - 40) / 5);               //步长按钮在进度条上占得比例
        sb_weight.setProgress((weight - 30) / 2);

        tv_step_length_vlaue.setText(step_length + getString(R.string.cm));
        tv_weight_value.setText(weight + getString(R.string.kg));
    }

    /**
     * 设置中SeekBar的拖动监听
     */
    private void listener() {
        sb_step_length.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onProgressChanged(SeekBar seekBar,
                                          int progress, boolean fromUser) {
                // TODO Auto-generated method stub
                step_length = progress * 5 + 40;
                tv_step_length_vlaue.setText(step_length
                        + getString(R.string.cm));
            }
        });
        sb_weight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                weight = progress * 2 + 30;
                tv_weight_value.setText(weight + getString(R.string.kg));
            }
        });
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save:
                editor.putInt(STEP_LENGTH_VALUE, step_length);
                editor.putInt(WEIGHT_VALUE, weight);
                editor.commit();

                Toast.makeText(SettingsActivity.this, "save success", Toast.LENGTH_SHORT).show();
                this.finish();
                break;
            case R.id.cancle:
                this.finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onRestart() {
        // TODO Auto-generated method stub
        super.onRestart();
        init();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        init();
    }
}
