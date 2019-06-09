package com.pchmn.materialchips.adapter;

import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.pchmn.materialchips.ChipView;
import com.pchmn.materialchips.ChipsInput;
import com.pchmn.materialchips.model.ChipInterface;
import com.pchmn.materialchips.util.ViewUtil;
import com.pchmn.materialchips.views.ChipsInputEditText;
import com.pchmn.materialchips.views.FilterableListView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ChipsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = ChipsAdapter.class.toString();
    private static final int TYPE_EDIT_TEXT = 0;
    private static final int TYPE_ITEM = 1;
    private Context mContext;
    private ChipsInput mChipsInput;
    private List<ChipInterface> mChipList = new ArrayList<>();
    private String mHintLabel;
    private ChipsInputEditText mEditText;
    private RecyclerView mRecycler;

    public ChipsAdapter(Context context, ChipsInput chipsInput, RecyclerView recycler) {
        mContext = context;
        mChipsInput = chipsInput;
        mRecycler = recycler;
        mHintLabel = mChipsInput.getHint();
        mEditText = mChipsInput.getEditText();
        initEditText();
    }

    private class ItemViewHolder extends RecyclerView.ViewHolder {

        private final ChipView chipView;

        ItemViewHolder(View view) {
            super(view);
            chipView = (ChipView) view;
        }
    }

    private class EditTextViewHolder extends RecyclerView.ViewHolder {

        private final EditText editText;

        EditTextViewHolder(View view) {
            super(view);
            editText = (EditText) view;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_EDIT_TEXT)
            return new EditTextViewHolder(mEditText);
        else
            return new ItemViewHolder(mChipsInput.getChipView());

    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        // edit text
        if (position == mChipList.size()) {
            if (mChipList.size() == 0)
                mEditText.setHint(mHintLabel);

            // auto fit edit text
            autofitEditText();
        }
        // chip
        else if (getItemCount() > 1) {
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            itemViewHolder.chipView.inflate(getItem(position));
            // handle click
            handleClickOnEditText(itemViewHolder.chipView, position);
        }
    }

    @Override
    public int getItemCount() {
        return mChipList.size() + 1;
    }

    private ChipInterface getItem(int position) {
        return mChipList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mChipList.size())
            return TYPE_EDIT_TEXT;

        return TYPE_ITEM;
    }

    @Override
    public long getItemId(int position) {
        return mChipList.get(position).hashCode();
    }

    private void initEditText() {
        mEditText.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mEditText.setHint(mHintLabel);
        mEditText.setBackgroundResource(android.R.color.transparent);
        // prevent fullscreen on landscape
        mEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mEditText.setPrivateImeOptions("nm");
        // no suggestion
        mEditText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // handle back space
        mEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // backspace
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    // remove last chip
                    if (mChipList.size() > 0 && mEditText.getText().toString().length() == 0)
                        removeChip(mChipList.size() - 1);
                }
                return false;
            }
        });

        // text changed
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mChipsInput.onTextChanged(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void autofitEditText() {
        ViewGroup.LayoutParams params = mEditText.getLayoutParams();
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mEditText.setLayoutParams(params);
        mEditText.setMinWidth(ViewUtil.dpToPx(50));

        mEditText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                int right = mRecycler.getRight();
                int left = mEditText.getLeft();
                ViewGroup.LayoutParams params = mEditText.getLayoutParams();

                if (left < (right - ViewUtil.dpToPx(100))) {
                    params.width = right - left;
                } else {
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                mEditText.setLayoutParams(params);
                mEditText.requestFocus();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    mEditText.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    mEditText.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }

        });
    }

    private void handleClickOnEditText(ChipView chipView, final int position) {
        // delete chip
        chipView.setOnDeleteClicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeChip(position);
            }
        });
    }

    public void setFilterableListView(FilterableListView filterableListView) {
        if (mEditText != null)
            mEditText.setFilterableListView(filterableListView);
    }

    public void addChip(ChipInterface chip) {
        if (!listContains(mChipList, chip)) {
            mChipList.add(chip);
            // notify listener
            mChipsInput.onChipAdded(chip, mChipList.size());
            // hide hint
            mEditText.setHint(null);
            // reset text
            mEditText.setText(null);
            // refresh data
            notifyItemInserted(mChipList.size());

            mRecycler.smoothScrollToPosition(getItemCount());
        }
    }

    public void removeChip(ChipInterface chip) {
        int position = mChipList.indexOf(chip);
        mChipList.remove(position);
        // notify listener
        notifyItemRangeChanged(position, getItemCount());
        // if 0 chip
        if (mChipList.size() == 0)
            mEditText.setHint(mHintLabel);
        // refresh data
        notifyDataSetChanged();
    }

    public void removeChip(int position) {
        ChipInterface chip = mChipList.get(position);
        // remove contact
        mChipList.remove(position);
        // notify listener
        mChipsInput.onChipRemoved(chip, mChipList.size());
        // if 0 chip
        if (mChipList.size() == 0)
            mEditText.setHint(mHintLabel);
        // refresh data
        notifyDataSetChanged();
    }

    public void removeChipById(Object id) {
        for (Iterator<ChipInterface> iter = mChipList.listIterator(); iter.hasNext(); ) {
            ChipInterface chip = iter.next();
            if (chip.getId() != null && chip.getId().equals(id)) {
                // remove chip
                iter.remove();
                // notify listener
                mChipsInput.onChipRemoved(chip, mChipList.size());
            }
        }
        // if 0 chip
        if (mChipList.size() == 0)
            mEditText.setHint(mHintLabel);
        // refresh data
        notifyDataSetChanged();
    }

    public void removeChipByLabel(String label) {
        for (Iterator<ChipInterface> iter = mChipList.listIterator(); iter.hasNext(); ) {
            ChipInterface chip = iter.next();
            if (chip.getLabel().equals(label)) {
                // remove chip
                iter.remove();
                // notify listener
                mChipsInput.onChipRemoved(chip, mChipList.size());
            }
        }
        // if 0 chip
        if (mChipList.size() == 0)
            mEditText.setHint(mHintLabel);
        // refresh data
        notifyDataSetChanged();
    }

    public void removeChipByInfo(String info) {
        for (Iterator<ChipInterface> iter = mChipList.listIterator(); iter.hasNext(); ) {
            ChipInterface chip = iter.next();
            if (chip.getInfo() != null && chip.getInfo().equals(info)) {
                // remove chip
                iter.remove();
                // notify listener
                mChipsInput.onChipRemoved(chip, mChipList.size());
            }
        }
        // if 0 chip
        if (mChipList.size() == 0)
            mEditText.setHint(mHintLabel);
        // refresh data
        notifyDataSetChanged();
    }

    public List<ChipInterface> getChipList() {
        return mChipList;
    }

    private boolean listContains(List<ChipInterface> contactList, ChipInterface chip) {

        if (mChipsInput.getChipValidator() != null) {
            for (ChipInterface item : contactList) {
                if (mChipsInput.getChipValidator().areEquals(item, chip))
                    return true;
            }
        } else {
            for (ChipInterface item : contactList) {
                if (chip.getId() != null && chip.getId().equals(item.getId()))
                    return true;
                if (chip.getLabel().equals(item.getLabel()))
                    return true;
            }
        }

        return false;
    }
}
