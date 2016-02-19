package com.marvin.uninstall360;
/**
 *  Copyright (C) 2016 MarvinR2D2

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	    //almost all qihoo apps' packagename start with "com.qihoo"
		//if there is exception,add here
	    private static final String[] START_QIHOO_APP = new String[]{
	    		"com.qihoo",
	    };

	    List<String> mDirtyPackageList = new ArrayList<String>();

	    List<String> mDefineDirtyPackageList = new ArrayList<String>();

	    private TextView mInfoTextView;

	    private final SimpleTask mSimpleTask = new SimpleTask() {


	        @Override
	        public void doInBackground() {

	            mDefineDirtyPackageList = new ArrayList<String>();
	            mDefineDirtyPackageList.addAll(Arrays.asList(START_QIHOO_APP));

	            mDirtyPackageList = new ArrayList<String>();

	            List<String> installedList = getPackageNameList();
	            for (int i = 0; i < installedList.size(); i++) {
	                String item = installedList.get(i);
	                if (isDirty(item)) {
	                    mDirtyPackageList.add(item);
	                }
	            }
	        }

	        private boolean isDirty(String packageName) {
	            for (int i = 0; i < mDefineDirtyPackageList.size(); i++) {
	                String item = mDefineDirtyPackageList.get(i);
	                if (packageName.startsWith(item)) {
	                    return true;
	                }
	            }
	            return false;
	        }

	        @Override
	        public void onFinish(boolean canceled) {
	            int size = mDirtyPackageList.size();
	            String ptn = getString(R.string.info_many);
	            String text = String.format(ptn, size);
	            mInfoTextView.setTextColor(Color.RED);
	            if (size == 0) {
	                text = getString(R.string.info_none);
	                mInfoTextView.setTextColor(Color.BLUE);
	            } else if (size == 1) {
	                text = getString(R.string.info_one);
	            }
	            mInfoTextView.setText(text);
	        }
	    };

	    private UnInstalledReceiver mUninstalledReceiver;

	    private void uninstallOneDirtyAPP() {
	        if (mDirtyPackageList.size() > 0) {
	            String first = mDirtyPackageList.get(0);
	            mDirtyPackageList.remove(0);
	            uninstall(first);
	        }
	    }

	    void checkDirtyPackage() {
	        mSimpleTask.restart();
	        SimpleExecutor.getInstance().execute(mSimpleTask);
	    }

	    @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_main);

	        mInfoTextView = (TextView) findViewById(R.id.text_view_3);
	        mInfoTextView.setOnClickListener(new View.OnClickListener() {
	            @Override
	            public void onClick(View v) {
	                uninstallOneDirtyAPP();
	            }
	        });
	    }

	    private List<String> getPackageNameList() {
	        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
	        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	        final List<ResolveInfo> infoList = getPackageManager().queryIntentActivities(mainIntent, 0);

	        HashMap<String, Boolean> packageNameList = new HashMap<String, Boolean>();
	        for (int i = 0; i < infoList.size(); i++) {
	            ResolveInfo info = infoList.get(i);
	            packageNameList.put(info.activityInfo.packageName, true);
	        }
	        List<String> list = new ArrayList<>();
	        Iterator<String> it = packageNameList.keySet().iterator();
	        while (it.hasNext()) {
	            list.add(it.next());
	        }
	        return list;
	    }


	    @Override
	    protected void onResume() {
	        super.onResume();
	        checkDirtyPackage();
	    }



	    private void uninstall(String packageName) {
	        Uri uri = Uri.parse("package:" + packageName);
	        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
	        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        startActivity(intent);
	    }


	    @Override
	    protected void onStart() {
	        super.onStart();
	        mUninstalledReceiver = new UnInstalledReceiver();
	        IntentFilter filter = new IntentFilter();
	        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
	        filter.addDataScheme("package");
	        this.registerReceiver(mUninstalledReceiver, filter);
	    }

	    @Override
	    protected void onDestroy() {
	        super.onDestroy();
	        if (mUninstalledReceiver != null) {
	            this.unregisterReceiver(mUninstalledReceiver);
	        }
	    }

	    /**
	     * uninstall
	     */
	    class UnInstalledReceiver extends BroadcastReceiver {
	        @Override
	        public void onReceive(Context context, final Intent intent) {
	            if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
	                runOnUiThread(new Runnable() {
	                    @Override
	                    public void run() {
	                        if (intent != null) {
	                            String packageNameRaw = intent.getDataString();
	                            if (packageNameRaw != null && !packageNameRaw.replaceAll(" ", "").equals("") && packageNameRaw.contains("package:")) {
	                                String packageName = packageNameRaw.replaceAll("package:", "");
	                                Toast.makeText(MainActivity.this, getString(R.string.uninstall_success) + " " + packageName, Toast.LENGTH_SHORT).show();
	                                uninstallOneDirtyAPP();
	                            }
	                        }
	                    }
	                });
	            }
	        }
	    }
}
