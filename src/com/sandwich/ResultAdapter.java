package com.sandwich;

import java.util.HashMap;

import com.sandwich.client.ResultListener;

import android.app.Activity;
import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ResultAdapter extends BaseAdapter {
	private SparseArray<ResultListener.Result> adapterTable;
	private HashMap<ResultListener.Result, Integer> mirror;
	private Context context;
	private int rowResourceId;
	
	public ResultAdapter(Context context, int rowResourceId)
	{
		this.context = context;
		this.rowResourceId = rowResourceId;
		this.adapterTable = new SparseArray<ResultListener.Result>();
		this.mirror = new HashMap<ResultListener.Result, Integer>();
	}
	
	public boolean add(ResultListener.Result result) {
		if (mirror.containsKey(result))
		{
			ResultListener.Result existingResult = adapterTable.get(mirror.get(result));
			existingResult.addPeers(result.peers);
		}
		else
		{
			adapterTable.put(adapterTable.size(), result);
			mirror.put(result, mirror.size());
			notifyDataSetChanged();
		}
		return true;
	}
	
	public ResultListener.Result remove(int id) {
		ResultListener.Result result = adapterTable.get(id);
		if (result != null)
		{
			adapterTable.remove(id);
			mirror.remove(result);
			notifyDataSetChanged();
		}
		return result;
	}
	
	public void clear() {
		adapterTable.clear();
		mirror.clear();
		notifyDataSetChanged();
	}
	
	public ResultListener.Result get(int id) {
		return adapterTable.get(id);
	}
	
	@Override
	public int getCount() {
		return adapterTable.size();
	}

	@Override
	public Object getItem(int index) {
		return adapterTable.get(index);
	}

	@Override
	public long getItemId(int id) {
		return id;
	}

	@Override
	public View getView(int id, View convertView, ViewGroup parent) {
		View row = convertView;
        
        if (row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(rowResourceId, parent, false);
        }
        
		TextView textView = (TextView) row;
		
		ResultListener.Result r = get(id);
		if (r == null)
			textView.setText("No results");
		else
			textView.setText(r.toString());
		
        return row;
	}
}
