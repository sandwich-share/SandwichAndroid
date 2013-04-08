package com.sandwich;

import java.util.HashSet;

import android.app.Activity;
import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ResultAdapter<T> extends BaseAdapter {
	private SparseArray<T> adapterTable;
	private HashSet<T> mirror;
	private Context context;
	private int rowResourceId;
	
	public ResultAdapter(Context context, int rowResourceId)
	{
		this.context = context;
		this.rowResourceId = rowResourceId;
		this.adapterTable = new SparseArray<T>();
		this.mirror = new HashSet<T>();
	}
	
	public boolean add(T t) {
		if (mirror.contains(t))
			return false;
		
		adapterTable.put(adapterTable.size(), t);
		mirror.add(t);
		
		return true;
	}
	
	public T remove(int id) {
		T t = adapterTable.get(id);
		adapterTable.remove(id);
		mirror.remove(t);
		return t;
	}
	
	public void clear() {
		adapterTable.clear();
		mirror.clear();
	}
	
	public T get(int id) {
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
        
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(rowResourceId, parent, false);
        }
        
		TextView textView = (TextView) row;
		textView.setText(get(id).toString());
		
        return row;
	}
	
}
