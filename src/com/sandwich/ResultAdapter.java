package com.sandwich;

import java.util.ArrayList;

import com.sandwich.client.ResultListener;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ResultAdapter extends BaseAdapter {
	private Context context;
	private int rowResourceId;
	private ArrayList<SearchTuple> cursors;
	private int count;
	
	public ResultAdapter(Context context, int rowResourceId)
	{
		this.context = context;
		this.rowResourceId = rowResourceId;
		this.cursors = new ArrayList<SearchTuple>();
	}

	public void add(SearchTuple c) {
		cursors.add(c);
		count += c.cursor.getCount();
		notifyDataSetChanged();
	}

	public void clear() {
		cursors.clear();
		notifyDataSetChanged();
	}
	
	private String getText(int id) {
		for (SearchTuple tuple : cursors)
		{
			if (id >= tuple.cursor.getCount())
				id -= tuple.cursor.getCount();
			else
			{
				tuple.cursor.moveToPosition(id);
				return tuple.cursor.getString(0);
			}
		}
		
		return null;
	}
	
	private ResultListener.Result get(int id) {
		for (SearchTuple tuple : cursors)
		{
			if (id >= tuple.cursor.getCount())
				id -= tuple.cursor.getCount();
			else
			{
				tuple.cursor.moveToPosition(id);
				return new ResultListener.Result(tuple.peer, tuple.cursor.getString(0), tuple.cursor.getInt(1));
			}
		}
		
		return null;
	}
	
	@Override
	public int getCount() {
		return count;
	}

	@Override
	public Object getItem(int index) {
		return get(index);
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
		textView.setText(getText(id));
		
        return row;
	}
	
	public static class SearchTuple {
		public Cursor cursor;
		public String peer;
		
		public SearchTuple(String peer, Cursor cursor)
		{
			this.cursor = cursor;
			this.peer = peer;
		}
	}
}
