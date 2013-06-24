package io.reign.data;

/**
 * 
 * @author ypai
 * 
 */
public interface DataPoint<T> {

    public String getKey();

    public T getValue();

    public Aggregation getAggregation();

    /**
     * 
     * @return how often this data item is aggregated in seconds; can be null if
     *         aggregation value is Aggregation.NONE
     */
    public Integer getAggregationIntervalSecs();
}