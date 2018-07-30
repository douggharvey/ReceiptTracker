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

import android.graphics.pdf.PdfRenderer;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.douglasharvey.receipttracker.R;
import com.douglasharvey.receipttracker.interfaces.TextRecognition;

import java.io.IOException;

import timber.log.Timber;

public class PageAdapter extends RecyclerView.Adapter<PageController> {
  private final LayoutInflater inflater;
  PdfRenderer renderer;
  private TextRecognition textRecognition;

  //public PageAdapter(LayoutInflater inflater, ParcelFileDescriptor pfd, TextRecognition textRecognition)
  public PageAdapter(LayoutInflater inflater, PdfRenderer renderer, TextRecognition textRecognition)
    throws IOException {
    this.inflater=inflater;
    this.renderer = renderer;
    //renderer=new PdfRenderer(pfd);
      Timber.d("PageAdapter: renderer created: no of pages: "+renderer.getPageCount());
    this.textRecognition = textRecognition;
  }

  @Override
  public PageController onCreateViewHolder(ViewGroup parent, int viewType) {
    return(new PageController(inflater.inflate(R.layout.page, parent, false), textRecognition));
  }

  @Override
  public void onBindViewHolder(PageController holder, int position) {
    PdfRenderer.Page page=renderer.openPage(position);
    holder.setPage(page, position);
    page.close();
  }

  @Override
  public int getItemCount() {
    return(renderer.getPageCount());
  }

  public void close() {
    renderer.close();
  }
}
