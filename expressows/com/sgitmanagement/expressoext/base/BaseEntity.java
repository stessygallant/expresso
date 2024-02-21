package com.sgitmanagement.expressoext.base;

import com.sgitmanagement.expresso.base.IEntity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

//@EntityListeners(AuditListener.class)

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@Access(value = AccessType.FIELD)
@MappedSuperclass
public class BaseEntity implements IEntity<Integer> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", updatable = false, nullable = false)
	private Integer id;

	public BaseEntity() {

	}

	public BaseEntity(BaseEntity baseEntity) {
		this.id = baseEntity.id;
	}

	public BaseEntity(Integer id) {
		this.id = id;
	}

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (id != null ? id.intValue() : 0);
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
		BaseEntity other = (BaseEntity) obj;
		if (id == null || other.id == null || id != other.id) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [id=" + id + "]";
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