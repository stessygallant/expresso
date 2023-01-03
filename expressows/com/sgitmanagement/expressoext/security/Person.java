package com.sgitmanagement.expressoext.security;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.annotations.Formula;

import com.sgitmanagement.expresso.base.Creatable;
import com.sgitmanagement.expresso.base.Updatable;
import com.sgitmanagement.expresso.util.JAXBDateAdapter;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@Entity
@Table(name = "person")
@Inheritance(strategy = InheritanceType.JOINED)
public class Person extends BasePerson implements Updatable, Creatable {

	@Column(name = "creation_user_id")
	private Integer creationUserId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "creation_user_id", insertable = false, updatable = false)
	private User creationUser;

	@Formula(value = "(SELECT CONCAT(p.first_name, \" \", p.last_name) FROM person p WHERE p.id = creation_user_id)")
	private String creationUserFullName;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "creation_date")
	private Date creationDate;

	@Column(name = "last_modified_user_id")
	private Integer lastModifiedUserId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "last_modified_user_id", insertable = false, updatable = false)
	private User lastModifiedUser;

	@Formula(value = "(SELECT CONCAT(p.first_name, \" \", p.last_name) FROM person p WHERE p.id = last_modified_user_id)")
	private String lastModifiedUserFullName;

	// @Version
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_modified_date")
	private Date lastModifiedDate;

	@Column(name = "phone_number")
	private String phoneNumber;

	@Column(name = "manager_id")
	private Integer managerId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "manager_id", insertable = false, updatable = false)
	private BasicPerson manager;

	@Column(name = "department_id")
	private Integer departmentId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "department_id", insertable = false, updatable = false)
	private Department department;

	@Column(name = "company_id")
	private Integer companyId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "company_id", insertable = false, updatable = false)
	private Company company;

	@Column(name = "job_title_id")
	private Integer jobTitleId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "job_title_id", insertable = false, updatable = false)
	private JobTitle jobTitle;

	public Person() {
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Override
	public String getLabel() {
		return getFullName();
	}

	public Integer getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(Integer departmentId) {
		this.departmentId = departmentId;
	}

	public Integer getManagerId() {
		return managerId;
	}

	public void setManagerId(Integer managerId) {
		this.managerId = managerId;
	}

	public Integer getJobTitleId() {
		return jobTitleId;
	}

	public void setJobTitleId(Integer jobTitleId) {
		this.jobTitleId = jobTitleId;
	}

	public BasicPerson getManager() {
		return manager;
	}

	@XmlElement
	public Department getDepartment() {
		return department;
	}

	@XmlElement
	public JobTitle getJobTitle() {
		return jobTitle;
	}

	public Integer getCompanyId() {
		return companyId;
	}

	public void setCompanyId(Integer companyId) {
		this.companyId = companyId;
	}

	@XmlElement
	public Company getCompany() {
		return company;
	}

	@Override
	public Integer getCreationUserId() {
		return creationUserId;
	}

	@Override
	public void setCreationUserId(Integer creationUserId) {
		this.creationUserId = creationUserId;
	}

	@Override
	public Date getCreationDate() {
		return creationDate;
	}

	@Override
	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	@Override
	public User getCreationUser() {
		return creationUser;
	}

	@XmlElement
	public String getCreationUserFullName() {
		return creationUserFullName;
	}

	@Override
	public Integer getLastModifiedUserId() {
		return lastModifiedUserId;
	}

	@Override
	public void setLastModifiedUserId(Integer lastModifiedUserId) {
		this.lastModifiedUserId = lastModifiedUserId;
	}

	@Override
	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	@Override
	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	@Override
	public User getLastModifiedUser() {
		return lastModifiedUser;
	}

	@Override
	@XmlElement
	public String getLastModifiedUserFullName() {
		return lastModifiedUserFullName;
	}

	@Override
	public String toString() {
		return "Person [phoneNumber=" + phoneNumber + ", managerId=" + managerId + ", manager=" + manager + ", departmentId=" + departmentId + ", department=" + department + ", companyId=" + companyId
				+ ", company=" + company + ", jobTitleId=" + jobTitleId + ", jobTitle=" + jobTitle + ", toString()=" + super.toString() + "]";
	}
}