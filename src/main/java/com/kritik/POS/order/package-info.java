@org.springframework.modulith.ApplicationModule(displayName = "Order", type = org.springframework.modulith.ApplicationModule.Type.OPEN, allowedDependencies = {
		"common",
		"exception",
		"security",
		"restaurant::api",
		"inventory::api",
		"tax::api",
		"configuredmenu::api"
})
package com.kritik.POS.order;
