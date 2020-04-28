package umn.ac.id;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class DetilActivity extends AppCompatActivity {
    private EditText etNim, etNama, etEmail, etNomorHP;
    int posisi;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detil);
        etNomorHP = findViewById(R.id.etNomorHp);
        etNim = findViewById(R.id.etNim);
        etNama = findViewById(R.id.etNama);
        etEmail = findViewById(R.id.etEmail);
        if( getIntent().getExtras() != null)
        {
            Mahasiswa mhs = (Mahasiswa) getIntent().getSerializableExtra("MAHASISWA");
            posisi = getIntent().getIntExtra("POSISI",0);
            etNim.setText(mhs.getNim());
            etNama.setText(mhs.getNama());
            etEmail.setText(mhs.getEmail());
            etNomorHP.setText(mhs.getNomorhp());
        }
    }
    public void simpanData(View view){
        String mNIM = etNim.getText().toString();
        String mNama = etNama.getText().toString();
        String mEmail = etEmail.getText().toString();
        String mNoHp = etNomorHP.getText().toString();
        if(mNIM.length() <= 0 || mNama.length() <= 0 ||
                mEmail.length() <= 0 || mNoHp.length() <= 0){
            Toast.makeText(this,"Semua harus Diisi",
                    Toast.LENGTH_LONG).show();
        } else {
            Intent intentJawab = new Intent();
            Mahasiswa mhs = new Mahasiswa(mNIM, mNama, mEmail, mNoHp);
            intentJawab.putExtra("MAHASISWA",mhs);
            intentJawab.putExtra("POSISI",posisi);
            setResult(RESULT_OK,intentJawab);
            finish();
        }
    }
    public void batal(View view){
        Intent intentJawab = new Intent();
        setResult(RESULT_CANCELED,intentJawab);
        finish();
    }
}
