package com.facebook.presto.block.uncompressed;

import com.facebook.presto.Range;
import com.facebook.presto.SizeOf;
import com.facebook.presto.Tuple;
import com.facebook.presto.TupleInfo;
import com.facebook.presto.block.Cursor;
import com.facebook.presto.block.Cursors;
import com.facebook.presto.slice.Slice;
import com.google.common.base.Preconditions;

import java.util.Iterator;

import static com.facebook.presto.SizeOf.SIZE_OF_DOUBLE;
import static com.facebook.presto.block.Cursor.AdvanceResult.FINISHED;
import static com.facebook.presto.block.Cursor.AdvanceResult.SUCCESS;

public class UncompressedDoubleCursor
        implements Cursor
{
    private final Iterator<UncompressedBlock> iterator;

    private UncompressedBlock block;
    private int index = -1;
    private int offset = -1;

    public UncompressedDoubleCursor(Iterator<UncompressedBlock> iterator)
    {
        Preconditions.checkNotNull(iterator, "iterator is null");
        Preconditions.checkArgument(iterator.hasNext(), "iterator is empty");
        this.iterator = iterator;
        block = iterator.next();
    }

    @Override
    public TupleInfo getTupleInfo()
    {
        return TupleInfo.SINGLE_DOUBLE;
    }

    @Override
    public Range getRange()
    {
        return Range.ALL;
    }

    @Override
    public boolean isValid()
    {
        return index >= 0 && block != null;
    }

    @Override
    public boolean isFinished()
    {
        return block == null;
    }

    @Override
    public AdvanceResult advanceNextValue()
    {
        if (block == null) {
            return FINISHED;
        }

        if (index < 0) {
            // next value is within the current block
            index = 0;
            offset = 0;
            return SUCCESS;
        }
        else if (index < block.getCount() - 1) {
            // next value is within the current block
            index++;
            offset += SIZE_OF_DOUBLE;
            return SUCCESS;
        }
        else if (iterator.hasNext()) {
            // next value is within the next block
            // advance to next block
            block = iterator.next();
            index = 0;
            offset = 0;
            return SUCCESS;
        }
        else {
            // no more data
            block = null;
            index = -1;
            offset = -1;
            return FINISHED;
        }
    }

    @Override
    public AdvanceResult advanceNextPosition()
    {
        return advanceNextValue();
    }

    @Override
    public AdvanceResult advanceToPosition(long newPosition)
    {
        Preconditions.checkArgument(index < 0 || newPosition >= getPosition(), "Can't advance backwards");

        if (block == null) {
            return FINISHED;
        }

        if (index >= 0 && newPosition == getPosition()) {
            // position to current position? => no op
            return SUCCESS;
        }

        // skip to block containing requested position
        if (index < 0 || newPosition > block.getRange().getEnd()) {
            while (newPosition > block.getRange().getEnd() && iterator.hasNext()) {
                block = iterator.next();
        }

            // is the position off the end of the stream?
            if (newPosition > block.getRange().getEnd()) {
                block = null;
                index = -1;
                offset = -1;
                return FINISHED;
            }

            // point to first entry in the block we skipped to
            index = 0;
            offset = 0;
        }

        // skip to index within block
        while (block.getRange().getStart() + index < newPosition) {
            index++;
            offset += SIZE_OF_DOUBLE;
        }

        return SUCCESS;
    }

    @Override
    public Tuple getTuple()
    {
        Cursors.checkReadablePosition(this);
        return new Tuple(block.getSlice().slice(offset, SizeOf.SIZE_OF_DOUBLE), TupleInfo.SINGLE_DOUBLE);
    }

    @Override
    public long getLong(int field)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getDouble(int field)
    {
        Cursors.checkReadablePosition(this);
        Preconditions.checkElementIndex(0, 1, "field");
        return block.getSlice().getDouble(offset);
    }

    @Override
    public Slice getSlice(int field)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getPosition()
    {
        Cursors.checkReadablePosition(this);
        return block.getRange().getStart() + index;
    }

    @Override
    public long getCurrentValueEndPosition()
    {
        return getPosition();
    }

    @Override
    public boolean currentTupleEquals(Tuple value)
    {
        Cursors.checkReadablePosition(this);
        Slice tupleSlice = value.getTupleSlice();
        // compare using long which is simpler
        return tupleSlice.length() == SIZE_OF_DOUBLE && block.getSlice().getLong(offset) == tupleSlice.getLong(0);
    }

}
