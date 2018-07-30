/***
 Copyright (c) 2016 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 Covered in detail in the book _The Busy Coder's Guide to Android Development_
 https://commonsware.com/Android
 */

package com.douglasharvey.receipttracker.adapters;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.douglasharvey.receipttracker.R;
import com.douglasharvey.receipttracker.interfaces.TextRecognition;

import timber.log.Timber;

class PageController extends RecyclerView.ViewHolder {
    private final SubsamplingScaleImageView iv;
    private Bitmap bitmap;
    private TextRecognition textRecognition;

    PageController(View itemView, TextRecognition textRecognition) {
        super(itemView);
        iv = itemView.findViewById(R.id.page);
        this.textRecognition = textRecognition;
    }

    void setPage(PdfRenderer.Page page, int pageNumber) {
        if (bitmap == null) {
            int height = 2000;
            int width = height * page.getWidth() / page.getHeight();

            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Timber.d("setPage: bitmap:"+bitmap.getHeight()+":" + bitmap.getWidth());
        }

        bitmap.eraseColor(0xFFFFFFFF);
        Timber.d("setPage: render");
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        Timber.d("setPage: after render");
        iv.resetScaleAndCenter();
        iv.setImage(ImageSource.cachedBitmap(bitmap));
        if (pageNumber == 0) textRecognition.runTextRecognition(bitmap);
    }
}
