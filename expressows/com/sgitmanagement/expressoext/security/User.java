package com.sgitmanagement.expressoext.security;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.sgitmanagement.expresso.audit.Auditable;
import com.sgitmanagement.expresso.audit.ForbidAudit;
import com.sgitmanagement.expresso.base.IUser;
import com.sgitmanagement.expresso.base.KeyField;
import com.sgitmanagement.expresso.util.DeserializeOnlyStringAdapter;
import com.sgitmanagement.expresso.util.JAXBDateAdapter;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@Entity
@Table(name = "user")
public class User extends Person implements IUser, Auditable {

	@Column(name = "username")
	@KeyField
	private String userName;

	@Column(name = "ext_key")
	private String extKey;

	@ForbidAudit
	@Column(name = "password")
	private String password;

	@Temporal(TemporalType.DATE)
	@Column(name = "password_expiration_date")
	private Date passwordExpirationDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "termination_date")
	private Date terminationDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "creation_date")
	private Date userCreationDate;

	@Column(name = "local_account")
	private boolean localAccount;

	@Column(name = "generic_account")
	private boolean genericAccount;

	@Column(name = "language")
	private String language;

	@Column(name = "note")
	private String note;

	@ForbidAudit
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_visit_date")
	private Date lastVisitDate;

	@ForbidAudit
	@Column(name = "nbr_failed_attempts")
	private int nbrFailedAttempts;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_role",
			// join
			joinColumns = { @JoinColumn(name = "user_id", referencedColumnName = "id") },
			// inverse
			inverseJoinColumns = { @JoinColumn(name = "role_id", referencedColumnName = "id") })
	private Set<Role> roles;

	@OneToMany(mappedBy = "user", cascade = { CascadeType.ALL }, orphanRemoval = true)
	private List<UserInfo> userInfos;

	// this is only use to create a user based on a person
	@Transient
	private Integer personId;

	public User() {
	}

	@Override
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@XmlJavaTypeAdapter(DeserializeOnlyStringAdapter.class)
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	// @XmlElement
	public List<UserInfo> getUserInfos() {
		return userInfos;
	}

	public UserInfo getUserInfo(Integer roleInfoId) {
		for (UserInfo userInfo : userInfos) {
			if (userInfo.getRoleInfoId().equals(roleInfoId)) {
				return userInfo;
			}
		}
		return null;
	}

	public UserInfo getUserInfo(String roleInfoPgmKey) {
		for (UserInfo userInfo : userInfos) {
			if (userInfo.getRoleInfo().getPgmKey().equals(roleInfoPgmKey)) {
				return userInfo;
			}
		}
		return null;
	}

	public String getExtKey() {
		return extKey;
	}

	public void setExtKey(String extKey) {
		this.extKey = extKey;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	@Override
	public String toString() {
		return "User [userName=" + userName + "]";
	}

	public Date getPasswordExpirationDate() {
		return passwordExpirationDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setPasswordExpirationDate(Date passwordExpirationDate) {
		this.passwordExpirationDate = passwordExpirationDate;
	}

	public boolean isLocalAccount() {
		return localAccount;
	}

	public void setLocalAccount(boolean localAccount) {
		this.localAccount = localAccount;
	}

	public Set<Role> getRoles() {
		if (roles == null) {
			roles = new HashSet<>();
		}
		return roles;
	}

	// only used to create a user based on an existing person
	public void setPersonId(Integer personId) {
		this.personId = personId;
	}

	public Integer getPersonId() {
		return personId;
	}

	public Date getLastVisitDate() {
		return lastVisitDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setLastVisitDate(Date lastVisitDate) {
		this.lastVisitDate = lastVisitDate;
	}

	public int getNbrFailedAttempts() {
		return nbrFailedAttempts;
	}

	public void setNbrFailedAttempts(int nbrFailedAttempts) {
		this.nbrFailedAttempts = nbrFailedAttempts;
	}

	public Date getTerminationDate() {
		return terminationDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setTerminationDate(Date terminationDate) {
		this.terminationDate = terminationDate;
	}

	public boolean isGenericAccount() {
		return genericAccount;
	}

	public void setGenericAccount(boolean genericAccount) {
		this.genericAccount = genericAccount;
	}

	public Date getUserCreationDate() {
		return userCreationDate;
	}

	@XmlJavaTypeAdapter(JAXBDateAdapter.class)
	public void setUserCreationDate(Date userCreationDate) {
		this.userCreationDate = userCreationDate;
	}

}
