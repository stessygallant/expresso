package com.sgitmanagement.expressoext.modif;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.sgitmanagement.expressoext.base.BaseOption;

@Entity
@Table(name = "required_approval_status")
public class RequiredApprovalStatus extends BaseOption {
}