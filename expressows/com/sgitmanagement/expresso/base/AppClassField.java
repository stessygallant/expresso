package com.sgitmanagement.expresso.base;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.sgitmanagement.expresso.dto.Query;

public class AppClassField {
	private String type;
	private String name;
	private String label;
	private String helpLabel;
	private String placeHolder;
	private Boolean editable;
	private Boolean nullable;
	private Boolean filterable;
	@SerializedName("transient")
	private Boolean fieldTransient;
	private Object defaultValue;
	private Boolean unique;
	private Boolean keyField;
	private Integer maxLength;
	private Boolean refreshable;
	private Boolean updatable;
	private Integer decimals;
	private Boolean allowNegative;
	private Boolean timestamp;
	private Boolean timeOnly;
	private Boolean multipleSelection;
	private Boolean lookupSelection;
	private Boolean hidden;
	private String restrictedRole;

	private Object /* true|String|Reference */ reference;
	private Object /* true|String|Values */ values;

	private String notes;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getPlaceHolder() {
		return placeHolder;
	}

	public void setPlaceHolder(String placeHolder) {
		this.placeHolder = placeHolder;
	}

	public Boolean getEditable() {
		return editable;
	}

	public void setEditable(Boolean editable) {
		this.editable = editable;
	}

	public Boolean getNullable() {
		return nullable;
	}

	public void setNullable(Boolean nullable) {
		this.nullable = nullable;
	}

	public Boolean getFilterable() {
		return filterable;
	}

	public void setFilterable(Boolean filterable) {
		this.filterable = filterable;
	}

	public Boolean getTransient() {
		return fieldTransient;
	}

	public void setTransient(Boolean fieldTransient) {
		this.fieldTransient = fieldTransient;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Object getReference() {
		return reference;
	}

	public void setReference(Object reference) {
		this.reference = reference;
	}

	public Object getValues() {
		return values;
	}

	public void setValues(Object values) {
		this.values = values;
	}

	public Boolean getUnique() {
		return unique;
	}

	public void setUnique(Boolean unique) {
		this.unique = unique;
	}

	public Boolean getKeyField() {
		return keyField;
	}

	public void setKeyField(Boolean keyField) {
		this.keyField = keyField;
	}

	public Integer getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
	}

	public Boolean getRefreshable() {
		return refreshable;
	}

	public void setRefreshable(Boolean refreshable) {
		this.refreshable = refreshable;
	}

	public Boolean getUpdatable() {
		return updatable;
	}

	public void setUpdatable(Boolean updatable) {
		this.updatable = updatable;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public void addNote(String note) {
		this.notes = (this.notes != null ? notes + ".\n" + note : note);
	}

	public Integer getDecimals() {
		return decimals;
	}

	public void setDecimals(Integer decimals) {
		this.decimals = decimals;
	}

	public Boolean getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Boolean timestamp) {
		this.timestamp = timestamp;
	}

	public Boolean getAllowNegative() {
		return allowNegative;
	}

	public void setAllowNegative(Boolean allowNegative) {
		this.allowNegative = allowNegative;
	}

	public Boolean getMultipleSelection() {
		return multipleSelection;
	}

	public void setMultipleSelection(Boolean multipleSelection) {
		this.multipleSelection = multipleSelection;
	}

	public Boolean getFieldTransient() {
		return fieldTransient;
	}

	public void setFieldTransient(Boolean fieldTransient) {
		this.fieldTransient = fieldTransient;
	}

	public Boolean getLookupSelection() {
		return lookupSelection;
	}

	public void setLookupSelection(Boolean lookupSelection) {
		this.lookupSelection = lookupSelection;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public String getHelpLabel() {
		return helpLabel;
	}

	public void setHelpLabel(String helpLabel) {
		this.helpLabel = helpLabel;
	}

	@Override
	public String toString() {
		return "AppClassField [type=" + type + ", label=" + label + ", helpLabel=" + helpLabel + ", placeHolder=" + placeHolder + ", editable=" + editable + ", nullable=" + nullable + ", filterable="
				+ filterable + ", fieldTransient=" + fieldTransient + ", defaultValue=" + defaultValue + ", unique=" + unique + ", maxLength=" + maxLength + ", refreshable=" + refreshable
				+ ", updatable=" + updatable + ", decimals=" + decimals + ", allowNegative=" + allowNegative + ", timestamp=" + timestamp + ", multipleSelection=" + multipleSelection
				+ ", lookupSelection=" + lookupSelection + ", hidden=" + hidden + ", reference=" + reference + ", values=" + values + ", notes=" + notes + "]";
	}

	public static class Option {
		private Object id;
		private String label;

		public Option() {

		}

		public Option(Object id, String label) {
			super();
			this.id = id;
			this.label = label;
		}

		public Object getId() {
			return id;
		}

		public void setId(Object id) {
			this.id = id;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

	}

	public static class Values {
		private List<Option> data;
		private String wsPath;
		private Query.Filter filter;
		private Boolean selectFirstOption;
		private String fieldName;

		public Values() {

		}

		public Values(String wsPath) {
			this.wsPath = wsPath;
		}

		public Values(List<Option> data) {
			this.data = data;
		}

		public List<Option> getData() {
			return data;
		}

		public void setData(List<Option> data) {
			this.data = data;
		}

		public String getWsPath() {
			return wsPath;
		}

		public void setWsPath(String wsPath) {
			this.wsPath = wsPath;
		}

		public Query.Filter getFilter() {
			return filter;
		}

		public void setFilter(Query.Filter filter) {
			this.filter = filter;
		}

		public Boolean getSelectFirstOption() {
			return selectFirstOption;
		}

		public void setSelectFirstOption(Boolean selectFirstOption) {
			this.selectFirstOption = selectFirstOption;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

	}

	public static class Reference {
		private String wsPath;
		private Query.Filter filter;

		private String fieldName;
		private String resourceName;
		private String resourceManagerDef;

		public Reference() {

		}

		public Reference(String wsPath) {
			this.wsPath = wsPath;
		}

		public String getWsPath() {
			return wsPath;
		}

		public void setWsPath(String wsPath) {
			this.wsPath = wsPath;
		}

		public Query.Filter getFilter() {
			return filter;
		}

		public void setFilter(Query.Filter filter) {
			this.filter = filter;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public String getResourceName() {
			return resourceName;
		}

		public void setResourceName(String resourceName) {
			this.resourceName = resourceName;
		}

		public String getResourceManagerDef() {
			return resourceManagerDef;
		}

		public void setResourceManagerDef(String resourceManagerDef) {
			this.resourceManagerDef = resourceManagerDef;
		}
	}

	public Boolean getTimeOnly() {
		return timeOnly;
	}

	public void setTimeOnly(Boolean timeOnly) {
		this.timeOnly = timeOnly;
	}

	public String getRestrictedRole() {
		return restrictedRole;
	}

	public void setRestrictedRole(String restrictedRole) {
		this.restrictedRole = restrictedRole;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
