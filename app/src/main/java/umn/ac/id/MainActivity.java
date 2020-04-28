package umn.ac.id;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import static android.provider.Contacts.SettingsColumns.KEY;

public class MainActivity extends AppCompatActivity {

    public static String server;
    private final int SETTING_REQUEST = 1;
    private SharedPreferences mPreferences;
    private String sharedPrefFile;
    private Context context;
    private RecyclerView rv;
    static MahasiswaListAdapter adapter;
    private static final int REQUEST_TAMBAH = 2;
    private static final int REQUEST_EDIT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        sharedPrefFile = context.getPackageName();
        mPreferences = getSharedPreferences(sharedPrefFile,MODE_PRIVATE);
        server = mPreferences.getString(KEY, "https://if633.mobdevumn.com/api/");
        rv = (RecyclerView) findViewById(R.id.rvMahasiswa);
        adapter = new MahasiswaListAdapter(this);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(this));
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
        }
        if (networkInfo != null && networkInfo.isConnected()){
            new AsyncAmbilSemuaMahasiswa().execute();
        } else {
            Toast.makeText(MainActivity.this, "Tidak ada Koneksi Jaringan", Toast.LENGTH_LONG).show();
        }
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addIntent = new Intent(MainActivity.this,
                        DetilActivity.class);
                startActivityForResult(addIntent,REQUEST_TAMBAH);
            }
        });
        ItemTouchHelper helper =
                new ItemTouchHelper(
                        new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT
                                | ItemTouchHelper.RIGHT) {
                            @Override
                            public boolean onMove(@NonNull RecyclerView recyclerView,
                                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                                  @NonNull RecyclerView.ViewHolder target) {
                                return false;
                            }
                            @Override
                            public void onSwiped(@NonNull RecyclerView.ViewHolder
                                                         viewHolder, int direction) {
                                int posisi = viewHolder.getAdapterPosition();
                                Mahasiswa mhs =
                                        adapter.getMahasiswaAtPosition(posisi);
                                if(direction == ItemTouchHelper.LEFT){
                                    new AsyncHapusMahasiswa(posisi).
                                            execute(mhs.getNim());
                                }

                                else if(direction == ItemTouchHelper.RIGHT) {
                                    Intent editIntet = new Intent(MainActivity.this,
                                            DetilActivity.class);
                                    editIntet.putExtra("MAHASISWA", mhs);
                                    editIntet.putExtra("POSISI",posisi);
                                    startActivityForResult(editIntet, REQUEST_EDIT);
                                }
                            }
                        }
                );
        helper.attachToRecyclerView(rv);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.activity_setting) {
            Intent settingIntent = new Intent(MainActivity.this,
                    SettingActivity.class);
            startActivityForResult(settingIntent,SETTING_REQUEST);
            return true;
        } else if(id == R.id.action_cari){
            Intent cariIntent = new Intent(MainActivity.this,
                    CariActivity.class);
            startActivity(cariIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onPause(){
        super.onPause();
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
        preferencesEditor.putString(KEY, server);
        preferencesEditor.apply();
    }
    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data){
        super.onActivityResult(reqCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(reqCode == SETTING_REQUEST){
                SharedPreferences.Editor preferencesEditor =
                        mPreferences.edit();
                preferencesEditor.putString(KEY, server);
                preferencesEditor.apply();
                Toast.makeText(context,
                        "Setting telah disimpan",Toast.LENGTH_LONG).show();
            } else {
                Mahasiswa mhs = (Mahasiswa)
                        data.getSerializableExtra("MAHASISWA");
                if (reqCode == REQUEST_TAMBAH) {
                    new AsyncTambahMahasiswa().execute(mhs);
                } else if (reqCode == REQUEST_EDIT) {
                    int posisi = data.getIntExtra("POSISI",-1);
                    new AsyncUpdateMahasiswa(posisi).execute(mhs);
                }
            }
        } else {
            adapter.notifyDataSetChanged();
        }
    }
    void tutupKoneksi(BufferedReader reader, HttpsURLConnection con){
        if (con != null) {
            con.disconnect();
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e("WEBSERVICE",
                        "Exception: "+e.getMessage().toString());
            }
        }
    }
    String sambungKeServer(String actionPage, String postString){
        HttpsURLConnection con = null;
        StringBuilder sb = null;
        BufferedReader reader = null;
        String urlParameters = postString;
        byte[] postData = urlParameters.getBytes(
                StandardCharsets.UTF_8 );
        int postDataLength = postData.length;
        try {
            sb = new StringBuilder();
            String urlString = MainActivity.server+actionPage;
            URL url = new URL(urlString);
            con = (HttpsURLConnection) url.openConnection();
            con.setReadTimeout(10000);
            con.setConnectTimeout(15000);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setInstanceFollowRedirects( false );
            con.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            con.setRequestProperty("charset", "utf-8");
            con.setRequestProperty("Content-Length",
                    Integer.toString( postDataLength ));
            con.setUseCaches( false );
            con.connect();
            DataOutputStream wr = new DataOutputStream(
                    con.getOutputStream());
            wr.write(postData);
            wr.flush();
            wr.close();
            InputStream is = con.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            if (sb.length() == 0) {
                return null;
            }
            String mhsJSONString = sb.toString();
            JSONObject jsonObject = new JSONObject(mhsJSONString);
            int sukses = (int) jsonObject.getInt("success");
            if(sukses == 1){
                return "sukses";
            } else {
                return null;
            }
        } catch (Exception e){
            Log.e("WEBSERVICE","Exception: "+e.getMessage().toString());
        } finally {
            tutupKoneksi(reader, con);
        }
        return null;
    }
    class AsyncAmbilSemuaMahasiswa extends AsyncTask<Void, Void,
            List<Mahasiswa>> {
        @Override
        protected List<Mahasiswa> doInBackground(Void... voids) {
            HttpsURLConnection con = null;
            StringBuilder sb = null;
            BufferedReader reader = null;
            List<Mahasiswa> mahasiswas = new ArrayList<>();
            try {
                sb = new StringBuilder();
                String urlString = MainActivity.server +
                        "ambil_semua_mahasiswa.php";
                URL url = new URL(urlString);
                con = (HttpsURLConnection) url.openConnection();
                con.connect();
                InputStream is = con.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                if (sb.length() == 0) {
                    return null;
                }
                String mhsJSONString = sb.toString();
                JSONObject jsonObject = new JSONObject(mhsJSONString);
                int sukses = (int) jsonObject.getInt("success");
                if(sukses == 1){
                    JSONArray mhsJSONArray = (JSONArray)
                            jsonObject.get("mahasiswas");
                    for(int i = 0 ; i < mhsJSONArray.length(); i++){
                        JSONObject mhsJSONobj = (JSONObject)
                                mhsJSONArray.get(i);
                        String nim = mhsJSONobj.getString("nim");
                        String nama = mhsJSONobj.getString("nama");
                        String email = mhsJSONobj.getString("email");
                        String nomorHp = mhsJSONobj.getString("nomorHp");
                        Mahasiswa mhs = new Mahasiswa(nim, nama, email,
                                nomorHp);
                        mahasiswas.add(mhs);
                    }
                    return mahasiswas;
                } else {
                    return null;
                }
            } catch (Exception e){
                Log.e("WEBSERVICE",
                        "Exception: "+e.getMessage().toString());
            } finally {
                tutupKoneksi(reader, con);
            }
            return null;
        }
        @Override
        protected void onPostExecute(List<Mahasiswa> mahasiswas) {
            super.onPostExecute(mahasiswas);
            adapter.setDaftarMahasiswa(mahasiswas);
        }
    }
    class AsyncHapusMahasiswa extends AsyncTask<String, Void, String>{
        private int mPosisi;
        AsyncHapusMahasiswa(int posisi){
            mPosisi = posisi;
        }
        @Override
        protected String doInBackground(String... strings) {
            return sambungKeServer("hapus_mahasiswa.php",
                    "nim=" + strings[0]);
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if( (s != null) && (s.equalsIgnoreCase("sukses"))){
                adapter.hapusData(mPosisi);
                Toast.makeText(MainActivity.this,
                        "Mahasiswa Berhasil dihapus",Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this,
                        "Mahasiswa gagal dihapus",Toast.LENGTH_LONG).show();
            }
        }
    }
    class AsyncTambahMahasiswa extends AsyncTask<Mahasiswa, Void, String>{
        Mahasiswa mhs;
        @Override
        protected String doInBackground(Mahasiswa... mahasiswas) {
            mhs = mahasiswas[0];
            String urlParameters = "nim="+mhs.getNim()+"&"+
                    "nama="+mhs.getNama()+"&"+
                    "email="+mhs.getEmail()+"&"+
                    "nomorHp="+mhs.getNomorhp();
            return sambungKeServer("tambah_mahasiswa.php", urlParameters);
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if( (s != null) && (s.equalsIgnoreCase("sukses"))){
                adapter.tambahData(mhs);
                Toast.makeText(MainActivity.this,
                        "Mahasiswa Berhasil ditambahkan",
                        Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(MainActivity.this,
                        "Mahasiswa gagal ditmbahkan",Toast.LENGTH_LONG).show();
            }
        }
    }
    class AsyncUpdateMahasiswa extends AsyncTask<Mahasiswa, Void, String>{
        Mahasiswa mhs;
        int mPosisi;
        AsyncUpdateMahasiswa(int posisi){
            mPosisi = posisi;
        }
        @Override
        protected String doInBackground(Mahasiswa... mahasiswas) {
            mhs = mahasiswas[0];
            String urlParameters = "nim="+mhs.getNim()+"&"+
                    "nama="+mhs.getNama()+"&"+
                    "email="+mhs.getEmail()+"&"+
                    "nomorHp="+mhs.getNomorhp();
            return sambungKeServer("update_mahasiswa.php",
                    urlParameters);
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if( (s != null) && (s.equalsIgnoreCase("sukses"))){
                adapter.updateData(mPosisi, mhs);
                Toast.makeText(MainActivity.this,
                        "Data Mahasiswa Berhasil diupdate",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this,
                        "Data Mahasiswa gagal diupdate",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
