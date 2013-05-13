package com.leslie.cal_tri;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Archive extends Activity {

	private ListView lvContent;
	private String idString;
	private DBCalTri databaseHelper;
	private final String[] columns = new String[] { DBCalTri.KEY_DATE,
			DBCalTri.KEY_ACTIVITY, String.valueOf(DBCalTri.KEY_DISTANCE),
			DBCalTri.KEY_COMMENTS };
	private final int[] listItemTextViews = new int[] { R.id.list_date,
			R.id.imgActivity, R.id.list_distance, R.id.list_comments };
	private final SimpleDateFormat databaseDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd");
	private final SimpleDateFormat outputDateFormat = new SimpleDateFormat(
			"dd-MM-yy");
	private SimpleCursorAdapter logAdapter;

	protected void onCreate(Bundle savedInstanceState) {
		
		
		//TODO:
		// Move back/forward month buttons and queries
		
		
		
		super.onCreate(savedInstanceState);
		// this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.archive_listview);
		lvContent = (ListView) findViewById(R.id.list);
		
		//current date
		String currentMonthYear = new SimpleDateFormat("MMMM yyyy").format(new Date());
		setTitle("Training from "+ currentMonthYear);

		// displaying SQL data
		databaseHelper = new DBCalTri(this);
		databaseHelper.open();
		logAdapter = getEntriesAdapter();
		lvContent.setAdapter(logAdapter);
		databaseHelper.close();
		lvContent.setEmptyView(findViewById(R.id.empty_list));
		//lvContent.setBackgroundResource(R.drawable.customshape);

		lvContent.setLongClickable(true);
		lvContent.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View myView,
					int position, long id) {
				final String entryId = String.valueOf(id);
				final View tempView = myView;
				AlertDialog deleteEditChoice = new AlertDialog.Builder(
						Archive.this).create();
				deleteEditChoice.setTitle("Modify Entry");
				deleteEditChoice.setButton2("Edit",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								editEntry(String.valueOf(entryId));
							}

						});

				deleteEditChoice.setButton("Delete",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {						
								deleteEntry(tempView, entryId);
							}
						});
				deleteEditChoice.show();
				return false;
			}
		});

		lvContent.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intentDetailed = new Intent(Archive.this,
						ArchiveDetailed.class);
				intentDetailed.putExtra("id", String.valueOf(id));
				startActivity(intentDetailed);
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.archive_menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_sort:
			// sort choices
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Sort Entries by: ");
			final String sortItems[] = { "Date (most recent)", "Date (oldest)",
					"Miles (descending)", "Miles (ascending)", "Activity Type" };
			builder.setItems(sortItems, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int choice) {
					databaseHelper.open();
					sortBy(choice);
					databaseHelper.close();
				}
			});
			AlertDialog sortDialog = builder.create();
			sortDialog.show();
			break;

		case R.id.menu_delete:
			Toast deleteNotificationToast = Toast.makeText(
					getApplicationContext(),
					"Please select an entry to be deleted", Toast.LENGTH_LONG);
			deleteNotificationToast.show();

			lvContent.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> myAdapter, View myView,
						int position, long id) {
					try {
						deleteEntry(myView, String.valueOf(id));
						final long tempId = id;

					} catch (Exception e) {
						Log.e("ERROR delete",
								"Error deleting entry, archive.class");
					}
				}
			});

			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	//@SuppressWarnings("deprecation")
	private SimpleCursorAdapter getEntriesAdapter() {
		// Attach all entries to adapter and output to listview
		// Cursor c = databaseHelper.fetchAllEntriesBasic();
		//Cursor c = databaseHelper.fetchEntriesCurrentWeek();
		Cursor c = databaseHelper.fetchEntriesPastMonth();
		startManagingCursor(c);
		SimpleCursorAdapter entriesAdapter = new SimpleCursorAdapter(this,
				R.layout.archive_items, c, columns, listItemTextViews);
		stopManagingCursor(c);
		return formatAdapter(entriesAdapter);
	}

	
	public void deleteEntry(final View tempView, String id) {
		idString = id;
		AlertDialog dialogDelete = new AlertDialog.Builder(Archive.this)
				.create();
		dialogDelete.setTitle("Confirm Deletion");
		dialogDelete.setMessage("Please confirm you wish to delete this entry");
		dialogDelete.setButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				databaseHelper.open();
				databaseHelper.deleteEntry(idString);
				databaseHelper.close();
				
				Animation anim = AnimationUtils.loadAnimation(tempView.getContext(),
                        android.R.anim.slide_out_right);
                anim.setDuration(500);
                tempView.startAnimation(anim);
                anim.getDuration();
				Intent currentIntent = getIntent();
				startActivity(currentIntent);
				finish();
			}
		});

		dialogDelete.setButton2("Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
		dialogDelete.show();
	}

	public void editEntry(String id) {

	}

	public void sortBy(int sortChoice) {

		Cursor c = null;
		switch (sortChoice) {
		case 0:
			c = databaseHelper.getEntriesDateRecent();
			break;
		case 1:
			c = databaseHelper.getEntriesDateOldest();
			break;
		case 2:
			c = databaseHelper.getEntriesMilesDescending();
			break;
		case 3:
			c = databaseHelper.getEntriesMilesAscending();
			break;
		case 4:
			c = databaseHelper.getEntriesByActivity();
			break;
		}

		// start and stop managing cursor depreciated in api11
		// possible issues for the two uses of both in this activity
		startManagingCursor(c);
		SimpleCursorAdapter entries = new SimpleCursorAdapter(this,
				R.layout.archive_items, c, columns, listItemTextViews);
		stopManagingCursor(c);
		formatAdapter(entries);
		lvContent.setAdapter(entries);

	}

	public SimpleCursorAdapter formatAdapter(SimpleCursorAdapter adapter) {
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int column) {

				// Formatting date output
				if (column == 1) {
					TextView tv = (TextView) view;
					String dateStr = cursor.getString(cursor
							.getColumnIndex("date"));
					try {
						String reformattedDate = outputDateFormat
								.format(databaseDateFormat.parse(dateStr));
						tv.setText(reformattedDate);
						// tv.setTextSize(8);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					return true;
				}
				
				// Activity type icons
				int viewId = view.getId();
				if (column == 2 && viewId == R.id.imgActivity){
					if (cursor.getString(column).equals("Swim")){
						ImageView displayImage = (ImageView) view;
						displayImage.setBackgroundResource(R.drawable.blue_swim);
					} else if(cursor.getString(column).equals("Cycle")){
						ImageView displayImage = (ImageView) view;
						displayImage.setBackgroundResource(R.drawable.green_cycle);
					} else if(cursor.getString(column).equals("Run")){
						ImageView displayImage = (ImageView) view;
						displayImage.setBackgroundResource(R.drawable.orange_run);
					}
				}

				// CUSTOMISE listview output here if needed

				return false;
			}
		});

		return adapter;
	}

}