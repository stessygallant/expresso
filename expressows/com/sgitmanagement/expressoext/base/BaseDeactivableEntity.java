package com.sgitmanagement.expressoext.base;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.sgitmanagement.expresso.base.Deactivable;
import com.sgitmanagement.expresso.util.JAXBDateAdapter;

@MappedSuperclass
public abstract class BaseDeactivableEntity extends BaseEntity implements Deactivable {
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