package com.inipage.translatetoemoji.utils;

public interface ItemValidatorInterface {
	/**
	 * Validates an item.
	 * @param item The item to validate.
	 * @return Null if validation succeeds; a message otherwise.
     */
	String validate(String item);
}
