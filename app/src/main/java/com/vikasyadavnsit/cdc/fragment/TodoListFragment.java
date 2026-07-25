package com.vikasyadavnsit.cdc.fragment;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.data.TodoItem;
import com.vikasyadavnsit.cdc.database.repository.TodoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TodoListFragment extends Fragment {

    private TodoAdapter adapter;
    private List<TodoItem> items = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todo_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.todo_recycler_view);
        EditText addEditText = view.findViewById(R.id.todo_add_edit_text);
        ImageView backButton = view.findViewById(R.id.todo_back_button);
        TextView addButton = view.findViewById(R.id.todo_add_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TodoAdapter(this::onToggle, this::onDelete);
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        addButton.setOnClickListener(v -> addTask(addEditText));
        addEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTask(addEditText);
                return true;
            }
            return false;
        });

        items = TodoRepository.getAll(requireContext());
        adapter.setItems(items);

        return view;
    }

    private void addTask(EditText editText) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) return;
        items.add(0, new TodoItem(text));
        editText.setText("");
        persist();
    }

    private void onToggle(TodoItem item) {
        item.done = !item.done;
        item.updatedAt = System.currentTimeMillis();
        persist();
    }

    private void onDelete(TodoItem item) {
        items.remove(item);
        persist();
    }

    private void persist() {
        TodoRepository.saveAll(requireContext(), items);
        adapter.setItems(items);
    }

    private static class TodoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        private List<Object> displayList = new ArrayList<>();
        private final OnToggleListener onToggle;
        private final OnDeleteListener onDelete;

        interface OnToggleListener { void onToggle(TodoItem item); }
        interface OnDeleteListener { void onDelete(TodoItem item); }

        TodoAdapter(OnToggleListener onToggle, OnDeleteListener onDelete) {
            this.onToggle = onToggle;
            this.onDelete = onDelete;
        }

        void setItems(List<TodoItem> items) {
            List<TodoItem> active = items.stream().filter(i -> !i.done).collect(Collectors.toList());
            List<TodoItem> done = items.stream().filter(i -> i.done).collect(Collectors.toList());

            displayList = new ArrayList<>();
            displayList.add("TASKS");
            displayList.addAll(active);
            if (!done.isEmpty()) {
                displayList.add("DONE");
                displayList.addAll(done);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return displayList.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                View v = inflater.inflate(R.layout.item_todo_header, parent, false);
                return new HeaderHolder(v);
            }
            View v = inflater.inflate(R.layout.item_todo, parent, false);
            return new ItemHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderHolder) {
                String label = (String) displayList.get(position);
                ((HeaderHolder) holder).text.setText("TASKS".equals(label) ? "Tasks" : "Completed");
            } else {
                TodoItem item = (TodoItem) displayList.get(position);
                ItemHolder h = (ItemHolder) holder;
                h.checkbox.setOnCheckedChangeListener(null);
                h.checkbox.setChecked(item.done);
                h.title.setText(item.title);
                if (item.done) {
                    h.title.setPaintFlags(h.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    h.title.setAlpha(0.45f);
                } else {
                    h.title.setPaintFlags(h.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    h.title.setAlpha(1.0f);
                }
                h.checkbox.setOnCheckedChangeListener((btn, checked) -> onToggle.onToggle(item));
                h.deleteBtn.setOnClickListener(v -> onDelete.onDelete(item));
            }
        }

        @Override
        public int getItemCount() {
            return displayList.size();
        }

        static class HeaderHolder extends RecyclerView.ViewHolder {
            TextView text;
            HeaderHolder(View v) {
                super(v);
                text = v.findViewById(R.id.todo_header_text);
            }
        }

        static class ItemHolder extends RecyclerView.ViewHolder {
            CheckBox checkbox;
            TextView title;
            ImageView deleteBtn;
            ItemHolder(View v) {
                super(v);
                checkbox = v.findViewById(R.id.todo_item_checkbox);
                title = v.findViewById(R.id.todo_item_title);
                deleteBtn = v.findViewById(R.id.todo_item_delete);
            }
        }
    }
}
