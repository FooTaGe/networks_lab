import java.util.LinkedList;
import java.util.List;


/**
 * Describes a simple range, with a start, an end, and a length
 */
class Range {
    private Long start;
    private Long end;

    Range(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    Long getStart() {
        return start;
    }

    Long getEnd() {
        return end;
    }

    Long getLength() {
        return end - start + 1;
    }
}

class RangeList {
    LinkedList<Range> m_ranges;

    void add(Range i_range){
    //TODO
        for (Range k: m_ranges) {
            if(k.getEnd() + 1 == i_range.getStart()){
                m_ranges.remove(k);
                m_ranges.add(new Range(k.getStart(), i_range.getEnd()));
                return;
            }
        }
        m_ranges.add(i_range);
    }

    public List<Range> getMissing(){
        //Todo
        List<Range> ans = new LinkedList<Range>();
        return null;
     }
}
