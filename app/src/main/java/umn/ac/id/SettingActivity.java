package umn.ac.id;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class SettingActivity extends AppCompatActivity {
    private EditText etServer;
    private Button btnSimpan;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        etServer = (EditText) findViewById(R.id.httpServer);
        etServer.setText(MainActivity.server);
        btnSimpan = (Button) findViewById(R.id.btnSimpanSetting);
        btnSimpan.setOnClickListener(new View.OnClickListener(){
            @Override public void onClick(View v){
                MainActivity.server = etServer.getText().toString();
                setResult(RESULT_OK); finish();
            }
        });
    }
}