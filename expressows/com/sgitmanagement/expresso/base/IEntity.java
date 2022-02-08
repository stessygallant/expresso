package com.sgitmanagement.expresso.base;

import java.io.Serializable;

public interface IEntity<I> extends Serializable {
	public I getId();

	public void setId(I id);

	public String getLabel();
}
