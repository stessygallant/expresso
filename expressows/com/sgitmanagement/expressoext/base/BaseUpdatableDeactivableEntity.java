package com.sgitmanagement.expressoext.base;

import java.util.Date;

import com.sgitmanagement.expresso.base.Deactivable;
import com.sgitmanagement.expresso.util.JAXBDateAdapter;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@MappedSuperclass
public abstract class BaseUpdatableDeactivableEntity extends BaseUpdatableEntity implements Deactivable {
	@Temporal(TemporalType.DATE)
	@Column(name = "deactivation_date")
	private Date deactivationDate;

	@Override
	public Date getDeactivationDate() {
		return deactivationDate;
	}

	@Override
	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setDeactivationDate(Date deactivationDate) {
		this.deactivationDate = deactivationDate;
	}
}