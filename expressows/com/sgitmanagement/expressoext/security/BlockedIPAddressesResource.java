package com.sgitmanagement.expressoext.security;

import com.sgitmanagement.expressoext.base.BaseEntitiesResource;
import com.sgitmanagement.expressoext.base.BaseEntityResource;
import com.sgitmanagement.expressoext.security.BlockedIPAddressesResource.BlockedIPAddressResource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

@Path("/blockedIPAddress")
public class BlockedIPAddressesResource extends BaseEntitiesResource<BlockedIPAddress, BlockedIPAddressService, BlockedIPAddressResource> {

	public BlockedIPAddressesResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		super(BlockedIPAddress.class, request, response, new BlockedIPAddressResource(request, response), BlockedIPAddressService.class);
	}

	static public class BlockedIPAddressResource extends BaseEntityResource<BlockedIPAddress, BlockedIPAddressService> {

		public BlockedIPAddressResource(HttpServletRequest request, HttpServletResponse response) {
			super(BlockedIPAddress.class, request, response);
		}
	}
}