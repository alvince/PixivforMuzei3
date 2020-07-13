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

package com.antony.muzei.pixiv.util

import android.view.View
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

@UiThread
internal fun snackbar(view: View, init: SnackbarOptions.() -> Unit) {
    SnackbarOptions().apply {
        init()

        val content = if (textRes != 0) view.context.getString(textRes) else text
        if (content.isEmpty()) {
            return
        }
        Snackbar.make(view, content, duration).show()
    }
}

internal class SnackbarOptions {
    var text: String = ""

    @StringRes
    var textRes: Int = 0

    @BaseTransientBottomBar.Duration
    var duration: Int = Snackbar.LENGTH_LONG
}
