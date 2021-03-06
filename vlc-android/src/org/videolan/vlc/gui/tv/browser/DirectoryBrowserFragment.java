/*
 * *************************************************************************
 *  NetworkBrowseFragment.java
 * **************************************************************************
 *  Copyright © 2015 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui.tv.browser;

import android.annotation.TargetApi;
import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import org.jetbrains.annotations.NotNull;
import org.videolan.medialibrary.media.MediaLibraryItem;
import org.videolan.medialibrary.media.MediaWrapper;
import org.videolan.vlc.ExternalMonitor;
import org.videolan.vlc.util.Constants;
import org.videolan.vlc.viewmodels.browser.BrowserModel;
import org.videolan.vlc.viewmodels.browser.BrowserModelKt;

import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import kotlin.Pair;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class DirectoryBrowserFragment extends MediaSortedFragment<BrowserModel> {

    public static final String TAG = "VLC/DirectoryBrowserFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this, new BrowserModel.Factory(requireContext(), mUri.toString(), BrowserModelKt.TYPE_FILE, mShowHiddenFiles)).get(BrowserModel.class);
        viewModel.getCategories().observe(this, new Observer<Map<String, List<MediaLibraryItem>>>() {
            @Override
            public void onChanged(@Nullable Map<String, List<MediaLibraryItem>> stringListMap) {
                if (stringListMap != null) update(stringListMap);
            }
        });
        ExternalMonitor.INSTANCE.getStorageUnplugged().observe(this, new Observer<Uri>() {
            @Override
            public void onChanged(Uri uri) {
                if (mUri != null && "file".equals(mUri.getScheme())) {
                    final String currentPath = mUri.getPath();
                    final String unpluggedPath = uri.getPath();
                    if (currentPath != null && unpluggedPath != null && currentPath.startsWith(unpluggedPath)) {
                        final Activity activity = getActivity();
                        if (activity != null) activity.finish();
                    }
                }
            }
        });
        viewModel.getDescriptionUpdate().observe(this, new Observer<Pair<Integer, String>>() {
            @Override
            public void onChanged(Pair<Integer, String> pair) {
                final int position = pair.component1();
                final ArrayObjectAdapter adapter = (ArrayObjectAdapter) getAdapter();
                int index = -1;
                for (int i = 0; i < adapter.size(); ++i) {
                    final ObjectAdapter objectAdapter = ((ListRow) adapter.get(i)).getAdapter();
                    if (position > index + objectAdapter.size()) index += objectAdapter.size();
                    else for (int j = 0; j < objectAdapter.size(); ++j) {
                        if (++index == position) objectAdapter.notifyItemRangeChanged(j, 1, Constants.UPDATE_DESCRIPTION);
                    }
                }
            }
        });
    }

    @Override
    public void onItemClicked(@NotNull Presenter.ViewHolder viewHolder, @NotNull Object item, @NotNull RowPresenter.ViewHolder viewHolder1, @NotNull Row row) {
        if (item instanceof MediaWrapper && ((MediaWrapper)item).getType() == MediaWrapper.TYPE_DIR) viewModel.saveList((MediaWrapper)item);
        super.onItemClicked(viewHolder, item, viewHolder1, row);
    }
}
