package com.sgitmanagement.expressoext.base;

import com.sgitmanagement.expresso.base.AbstractBaseEntityService;
import com.sgitmanagement.expresso.base.Creatable;
import com.sgitmanagement.expresso.base.ExternalEntity;
import com.sgitmanagement.expresso.base.IUser;
import com.sgitmanagement.expresso.base.Updatable;
import com.sgitmanagement.expresso.util.ProgressSender;

public interface ExternalInterface<E extends Updatable & Creatable & ExternalEntity<I>, S extends AbstractBaseEntityService<E, U, I>, U extends IUser, I> {
	public void setService(S service);

	public void setTypeOfE(Class<E> typeOfE);

	public void sync(String section, ProgressSender progressSender, int progressWeight) throws Exception;

	public void delete(E e, boolean async) throws Exception;

	public void merge(E e, boolean async) throws Exception;

	public boolean isSynchronizing();
}
