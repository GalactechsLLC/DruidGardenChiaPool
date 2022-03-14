package garden.druid.pool.types;

public class Dataset<T, Y> {
	
	private T label;
	private Y data;
	
	public Dataset(T label, Y data) {
		super();
		this.label = label;
		this.data = data;
	}
	
	public T getLabel() {
		return label;
	}
	public void setLabel(T label) {
		this.label = label;
	}
	public Y getData() {
		return data;
	}
	public void setData(Y data) {
		this.data = data;
	}
	
	
}
