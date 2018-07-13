package com.douglasharvey.receipttracker.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.douglasharvey.receipttracker.R;
import com.douglasharvey.receipttracker.data.Receipt;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ReceiptListAdapter extends RecyclerView.Adapter<ReceiptListAdapter.ReceiptViewHolder> {
    Context context;

    class ReceiptViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tv_company)
        TextView tvCompany;
        @BindView(R.id.tv_date)
        TextView tvDate;
        @BindView(R.id.tv_amount)
        TextView tvAmount;
        @BindView(R.id.tv_payment_type)
        TextView tvPaymentType;
        @BindView(R.id.tv_category)
        TextView tvCategory;
        @BindView(R.id.tv_comment)
        TextView tvComment;

        private ReceiptViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private final LayoutInflater inflater;
    private List<Receipt> receipts;

    public ReceiptListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        this.context = context;
    }

    @Override
    public ReceiptViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.receipt_item, parent, false);
        return new ReceiptViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ReceiptViewHolder holder, int position) {
        if (receipts != null) {
            String[] categoryArray = context.getResources().getStringArray(R.array.category_array);
            String[] paymentTypeArray = context.getResources().getStringArray(R.array.payment_type_array);

            Receipt current = receipts.get(position);
            holder.tvCompany.setText(current.getCompany());
            holder.tvDate.setText(new SimpleDateFormat("dd/MM/yy").format(current.getReceiptDate()));
            holder.tvAmount.setText(new DecimalFormat("#####0.00").format(current.getAmount()));
            holder.tvCategory.setText(categoryArray[current.getCategory()]);
            holder.tvComment.setText(current.getComment());
            holder.tvPaymentType.setText(paymentTypeArray[current.getType()]);
        } else {
            // Covers the case of data not being ready yet.
            holder.tvCompany.setText("No Receipt");
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
