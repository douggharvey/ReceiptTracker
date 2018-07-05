package com.douglasharvey.receipttracker.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.douglasharvey.receipttracker.R;
import com.douglasharvey.receipttracker.data.Receipt;

import java.util.List;

public class ReceiptListAdapter extends RecyclerView.Adapter<ReceiptListAdapter.ReceiptViewHolder> {

    class ReceiptViewHolder extends RecyclerView.ViewHolder {
        private final TextView companyItemView;

        private ReceiptViewHolder(View itemView) {
            super(itemView);
            //TODO butterknife
            companyItemView = itemView.findViewById(R.id.company);
        }
    }

    private final LayoutInflater inflater;
    private List<Receipt> receipts;

    public ReceiptListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @Override
    public ReceiptViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.receipt_item, parent, false);
        return new ReceiptViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ReceiptViewHolder holder, int position) {
        if (receipts != null) {
            Receipt current = receipts.get(position);
            holder.companyItemView.setText(current.getCompany()+current.getReceiptDate()); //todo own field, format properly - consider time
        } else {
            // Covers the case of data not being ready yet.
            holder.companyItemView.setText("No Receipt");
        }
    }

    public void setReceipts(List<Receipt> receipts) {
        this.receipts = receipts;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (receipts != null)
            return receipts.size();
        else return 0;
    }
}
