/*
 *     This file is part of PixivforMuzei3.
 *
 *     PixivforMuzei3 is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program  is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.antony.muzei.pixiv.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.antony.muzei.pixiv.R;
import com.antony.muzei.pixiv.ui.fragments.SectionsPagerAdapter;
import com.google.android.apps.muzei.api.MuzeiContract;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Autogenerated code that sets up the Activity and the tabs
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
		ViewPager viewPager = findViewById(R.id.view_pager);
		viewPager.setAdapter(sectionsPagerAdapter);
		TabLayout tabs = findViewById(R.id.tabs);
		tabs.setupWithViewPager(viewPager);

		// If Muzei is not installed, this will redirect the user to Muzei's Play Store listing
		if (!isMuzeiInstalled())
		{
			// TODO localize these strings
			new AlertDialog.Builder(this)
					.setTitle(getApplicationContext().getString(R.string.dialogTitle_muzeiNotInstalled))
					.setMessage(getApplicationContext().getString(R.string.dialog_installMuzei))
					.setPositiveButton(android.R.string.yes, (dialog, which) ->
					{
						try
						{
							startActivity(new Intent(Intent.ACTION_VIEW,
									Uri.parse("market://details?id=net.nurik.roman.muzei")));
						} catch (android.content.ActivityNotFoundException ex)
						{
							startActivity(new Intent(Intent.ACTION_VIEW,
									Uri.parse("https://play.google.com/store/apps/details?id=net.nurik.roman.muzei")));
						}
					})
					.setNegativeButton(android.R.string.no, null)
					.show();
		}

		if (!isProviderSelected())
		{
			// TODO localize these strings
			new AlertDialog.Builder(this)
					.setTitle(getApplicationContext().getString(R.string.dialogTitle_muzeiNotActiveSource))
					.setMessage(getApplicationContext().getString(R.string.dialog_selectSource))
					.setNeutralButton(android.R.string.ok, null)
					.show();
		}
//		FloatingActionButton fab = findViewById(R.id.fab);
//
//		fab.setOnClickListener(new View.OnClickListener()
//		{
//			@Override
//			public void onClick(View view)
//			{
//				Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//						.setAction("Action", null).show();
//			}
//		});
	}

	// Checks if Muzei is installed
	private boolean isMuzeiInstalled()
	{
		boolean found = true;
		try
		{
			getApplicationContext().getPackageManager().getPackageInfo("net.nurik.roman.muzei", 0);
		} catch (PackageManager.NameNotFoundException ex)
		{
			found = false;
		}
		return found;
	}

	// Does a check to see if PixivForMuzei3 is selected as the active provider in Muzei
	private boolean isProviderSelected()
	{
		Cursor authorityCursor = getApplicationContext()
				.getContentResolver()
				.query(MuzeiContract.Sources.getContentUri(),
						new String[]{MuzeiContract.Sources.COLUMN_NAME_AUTHORITY},
						null,
						null,
						null);

		int authorityColumn = authorityCursor.getColumnIndex(MuzeiContract.Sources.COLUMN_NAME_AUTHORITY);
		while (authorityCursor.moveToNext())
		{
			String selectedAuthority = authorityCursor.getString(authorityColumn);
			if (selectedAuthority.equals("com.antony.muzei.pixiv.provider"))
			{
				authorityCursor.close();
				return true;
			}
		}
		authorityCursor.close();
		return false;
	}
}