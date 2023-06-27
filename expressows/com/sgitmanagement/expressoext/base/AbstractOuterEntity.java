package com.sgitmanagement.expressoext.base;

import com.sgitmanagement.expresso.base.IEntity;

import jakarta.persistence.MappedSuperclass;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@MappedSuperclass
abstract public class AbstractOuterEntity<I> implements IEntity<I> {
	public AbstractOuterEntity() {

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getId().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		@SuppressWarnings("unchecked")
		AbstractOuterEntity<I> other = (AbstractOuterEntity<I>) obj;
		if (getId() == null || other.getId() == null) {
			return false;
		}

		if (!getId().equals(other.getId())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [id=" + getId() + "]";
	}

	/**
	 * All entity must provide a label
	 *
	 * @return
	 */
	@Override
	@XmlElement
	public String getLabel() {
		return "" + getId();
	}
}