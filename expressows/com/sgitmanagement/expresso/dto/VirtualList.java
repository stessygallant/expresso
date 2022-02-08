package com.sgitmanagement.expresso.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class VirtualList<E> {
	private List<E> data;
	private long total;

	public VirtualList() {

	}

	public VirtualList(List<E> data, long total) {
		super();
		this.data = data;
		this.total = total;
	}

	public VirtualList(Collection<E> data) {
		this(new ArrayList<>(data));
	}

	public VirtualList(List<E> data) {
		this(data, data.size());
	}

	public VirtualList(E[] data) {
		this(Arrays.asList(data), data.length);
	}

	public List<E> getData() {
		return data;
	}

	public void setData(List<E> data) {
		this.data = data;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	@Override
	public String toString() {
		return "GridList [total=" + total + "]";
	}

}
