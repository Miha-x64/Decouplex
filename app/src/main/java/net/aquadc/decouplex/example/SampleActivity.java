package net.aquadc.decouplex.example;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import net.aquadc.decouplex.DecouplexBuilder;
import net.aquadc.decouplex.R;
import net.aquadc.decouplex.android.DecouplexActivity;
import net.aquadc.decouplex.annotation.OnResult;

import java.util.Locale;

public class SampleActivity extends DecouplexActivity {

    private SampleTask task;

    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        task = new DecouplexBuilder<SampleTask>()
                .face(SampleTask.class)
                .impl(new SampleTaskImpl())
                .create(this);

        result = (TextView) findViewById(R.id.result);
    }

    void test(View v) {
        task.getSquare(5);
    }

    @OnResult(face = SampleTask.class, method = "getSquare")
    protected void onResult(int i) {
        result.setText(String.format(Locale.getDefault(), "result: %s", i));
    }
}
