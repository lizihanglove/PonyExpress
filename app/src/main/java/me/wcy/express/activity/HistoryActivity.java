package me.wcy.express.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.sql.SQLException;
import java.util.List;

import butterknife.Bind;
import me.wcy.express.R;
import me.wcy.express.adapter.HistoryListAdapter;
import me.wcy.express.database.History;
import me.wcy.express.model.ExpressInfo;
import me.wcy.express.model.QueryResult;
import me.wcy.express.request.JSONRequest;
import me.wcy.express.util.DataManager;
import me.wcy.express.util.Utils;
import me.wcy.express.widget.MyAlertDialog;
import me.wcy.express.widget.MyProgressDialog;

public class HistoryActivity extends BaseActivity implements
        OnItemClickListener, OnItemLongClickListener {
    @Bind(R.id.history_list)
    ListView historyListView;
    @Bind(R.id.nothing)
    LinearLayout nothing;

    private MyProgressDialog mProgressDialog;
    private MyAlertDialog mAlertDialog;
    private ExpressInfo mExpressInfo;
    private RequestQueue mRequestQueue;
    private DataManager mDataManager;
    private List<History> mHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history);

        mProgressDialog = new MyProgressDialog(this);
        mRequestQueue = Volley.newRequestQueue(this);
        mDataManager = DataManager.getInstance().setContext(this);
        mExpressInfo = new ExpressInfo();
    }

    private void init() {
        try {
            mHistoryList = mDataManager.getHistoryList();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        historyListView.setAdapter(new HistoryListAdapter(this, mHistoryList));
        historyListView.setOnItemClickListener(this);
        historyListView.setOnItemLongClickListener(this);
        if (mHistoryList.size() == 0) {
            nothing.setVisibility(View.VISIBLE);
        } else {
            nothing.setVisibility(View.GONE);
        }
    }

    private void query() {
        if (!Utils.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.network_error, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        mProgressDialog.show();
        mProgressDialog.setMessage(getResources().getString(R.string.querying));
        JSONRequest<QueryResult> request = new JSONRequest<>(
                Utils.getQueryUrl(mExpressInfo), QueryResult.class,
                new Response.Listener<QueryResult>() {

                    @Override
                    public void onResponse(QueryResult queryResult) {
                        Log.i("Query", queryResult.getMessage());
                        mProgressDialog.cancel();
                        onQuerySuccess(queryResult);
                    }
                }, new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Query", error.getMessage(), error);
                mProgressDialog.cancel();
                Toast.makeText(HistoryActivity.this,
                        R.string.system_busy, Toast.LENGTH_SHORT)
                        .show();
            }
        });
        request.setShouldCache(false);
        mRequestQueue.add(request);
    }

    private void onQuerySuccess(QueryResult queryResult) {
        Intent intent = new Intent();
        intent.setClass(this, ResultActivity.class);
        queryResult.setCompany_name(mExpressInfo.getCompany_name());
        queryResult.setCompany_icon(mExpressInfo.getCompany_icon());
        queryResult.setNu(mExpressInfo.getPost_id());
        intent.putExtra(QueryActivity.QUERY_RESULT, queryResult);
        startActivity(intent);
        if (queryResult.getStatus().equals("200")) {
            mExpressInfo.setIs_check(queryResult.getIscheck());
        } else {
            mExpressInfo.setIs_check("0");
        }
        try {
            mDataManager.updateHistory(mExpressInfo);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> view, View arg1, int position, long arg3) {
        mExpressInfo.setPost_id(mHistoryList.get(position).getPost_id());
        mExpressInfo.setCompany_param(mHistoryList.get(position).getCompany_param());
        mExpressInfo.setCompany_name(mHistoryList.get(position).getCompany_name());
        mExpressInfo.setCompany_icon(mHistoryList.get(position).getCompany_icon());
        query();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> view, View arg1, int position, long arg3) {
        final int which = position;
        mAlertDialog = new MyAlertDialog(this);
        mAlertDialog.show();
        mAlertDialog.setTitle(getResources().getString(R.string.tips));
        mAlertDialog.setMessage(getResources().getString(
                R.string.sure_delete_history));
        mAlertDialog.setPositiveButton(getResources().getString(R.string.sure),
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mAlertDialog.cancel();
                        try {
                            mDataManager.deleteById(mHistoryList.get(which)
                                    .getPost_id());
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        init();
                    }
                });
        mAlertDialog.setNegativeButton(
                getResources().getString(R.string.cancle),
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mAlertDialog.cancel();
                    }
                });
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

}
