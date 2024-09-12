package com.example.likesong;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    int dimension = 10;
    int start = 1, end = 1;
    boolean freeze = false;
    private Spinner spinner;
    Cell[] cells;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView t = findViewById(R.id.textView);
        Button submit = findViewById(R.id.button1);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(freeze){
                    freeze = false; // unfreeze if we reset
                    t.setText(""); // get rid of no path comment, if there was one
                    submit.setText("FIND PATH");
                    for(Cell cell : cells)
                        if(cell!= null) cell.setWalkableChangeColor(true);
                }
                else if(start != end){
                    ArrayList<Integer> i = findPath(start, end);
                    if(i == null) {
                        t.setText("NO PATH");
                    }
                    else {
                        for (Integer h : i.subList(1, i.size() - 1))
                            findViewById(h).setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                    }
                    freeze = true;
                    submit.setText("RESET");
                }
            }
        });
        render();
    }

    private void render(){
        spinner = findViewById(R.id.spinner);
        String[] items = {"Set Walls", "Set Start", "Set End"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        ConstraintLayout layout = findViewById(R.id.layout);

        int walkable_color = getResources().getColor(android.R.color.holo_orange_light);
        int wall_color = getResources().getColor(android.R.color.holo_orange_dark);
        Cell view;
        ConstraintLayout.LayoutParams lp;
        int id;
        int[][] ids = new int[dimension][dimension];
        cells = new Cell[dimension*dimension+1];
        ConstraintSet cs = new ConstraintSet();

        for (int col = 0; col < dimension; col++) {
            for (int row = 0; row < dimension; row++) {
                view = new Cell(this);
                lp = new ConstraintLayout.LayoutParams(ConstraintSet.MATCH_CONSTRAINT, ConstraintSet.MATCH_CONSTRAINT);
                ids[col][row] = View.generateViewId();
                view.setId(ids[col][row]);
                view.setGravity(Gravity.CENTER);
                view.setBackgroundColor(view.isWalkable() ? walkable_color : wall_color);
                layout.addView(view, lp);
                cells[col * dimension + row + 1] = view;
            }
        }
        cs.clone(layout);
        cs.setDimensionRatio(R.id.gridFrame, dimension + ":" + dimension);
        for (int iRow = 0; iRow < dimension; iRow++) {
            for (int iCol = 0; iCol < dimension; iCol++) {
                id = ids[iRow][iCol];
                cs.setDimensionRatio(id, "1:1");
                if (iRow == 0) {
                    cs.connect(id, ConstraintSet.TOP, R.id.gridFrame, ConstraintSet.TOP);
                } else {
                    cs.connect(id, ConstraintSet.TOP, ids[iRow - 1][0], ConstraintSet.BOTTOM);
                }
            }
            cs.createHorizontalChain(R.id.gridFrame, ConstraintSet.LEFT, R.id.gridFrame, ConstraintSet.RIGHT, ids[iRow], null, ConstraintSet.CHAIN_PACKED);
        }
        cs.applyTo(layout);
    }

    PriorityQueue id_list = new PriorityQueue();
    ArrayList[] discoveredPath;
    ArrayList<Integer> ids;
    public ArrayList<Integer> findPath(int startID, int endID) {
        discoveredPath = new ArrayList[dimension*dimension+1];
        ids = new ArrayList<>(1000); // make this a queue
        discoveredPath[startID] = new ArrayList<>(); // path to get to startID
        discoveredPath[startID].add(startID);
        ids.add(startID);
        int current_id = startID;
        while(ids.size()>0){
            System.out.println("Cell " + cells[current_id].getId() + " vs " + current_id);
            current_id = ids.get(0);
            cells[current_id].setWalkable(false); // no backtracking
            adjacentPoints(current_id);
            ids.remove(0);
            if(current_id == endID) break;
            discoveredPath[current_id] = null;
        }
        return discoveredPath[current_id];
    }

    private void adjacentPoints(int id){
        int nextL = (id % dimension == 1) ? -1 : id - 1; // test if on left edge
        int nextR = (id % dimension == 0) ? -1 : id + 1; // test if on right edge
        int nextU = (id <= dimension) ? -1 : id - dimension; // test if on upper edge
        int nextD = (id > dimension * (dimension - 1)) ? -1 : id + dimension; // test if on bottom edge
        int[] adjacents = new int[]{nextL, nextR, nextU, nextD};

        for(int i : adjacents){
            System.out.println(i);
            if(i > 0 && cells[i].isWalkable() && discoveredPath[i]==null){
                ids.add(i);
                discoveredPath[i] = (ArrayList<Integer>)discoveredPath[id].clone();
                discoveredPath[i].add(i);
            }
        }
    }


    public class Cell extends AppCompatTextView {
        private boolean walkable; // false when square is wall, true otherwise
        private final int walkable_color = getResources().getColor(android.R.color.holo_orange_light);
        private int special_color = getResources().getColor(android.R.color.holo_orange_dark);

        public Cell(Context c) {
            super(c);
            walkable = true;
        }

        public boolean isWalkable(){
            return this.walkable;
        }

        public void setWalkable(boolean walkable){
            this.walkable = walkable;
        }

        private void setWalkableChangeColor(boolean walkable){
            this.walkable = walkable;
            setBackgroundColor(this.walkable ? walkable_color : special_color);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if(freeze) return false;
            super.onTouchEvent(e);
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_UP:
                    performClick();
                    return true;
            }
            return false;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            long selected = spinner.getSelectedItemId();
            if(selected==0) special_color = getResources().getColor(android.R.color.holo_orange_dark);
            else if(selected == 1){
                special_color = getResources().getColor(android.R.color.holo_green_dark);
                ((Cell)MainActivity.this.findViewById(start)).setWalkableChangeColor(true);
                start = this.getId();
            }
            else if(selected == 2){
                special_color = getResources().getColor(android.R.color.holo_red_dark);
                ((Cell)MainActivity.this.findViewById(end)).setWalkableChangeColor(true);
                end = this.getId();
            }
            setWalkableChangeColor(!walkable);
            if(selected == 1 || selected == 2) setWalkable(true); // start and end squares are walkable
            System.out.println(isWalkable());
            return true;
        }
    }
}

