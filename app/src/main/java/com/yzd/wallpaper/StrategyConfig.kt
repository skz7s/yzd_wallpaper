package com.yzd.wallpaper

class StrategyConfig {
    val strategyConfig = """
{
	"动漫壁纸[来自wallheaven]": {
		"crawl": {
			"crawl": {
				"method": "get",
				"result": {
					"url": {
						"xpath": "//*[@id='wallpaper']/@src"
					}
				},
				"url": {
					"xpath": "//a[@class='preview']/@href"
				}
			},
			"data": {
				"atleast": "1080x1920",
				"ratios": "portrait",
				"categories": "010",
				"order": "desc",
				"page": "{page}",
				"purity": "110",
				"sorting": "random"
			},
			"max_page": 1000,
			"method": "get",
			"url": "https://wallhaven.cc/search"
		}
	},
	"景物壁纸[来自wallheaven]": {
		"crawl": {
			"crawl": {
				"method": "get",
				"result": {
					"url": {
						"xpath": "//*[@id='wallpaper']/@src"
					}
				},
				"url": {
					"xpath": "//a[@class='preview']/@href"
				}
			},
			"data": {
				"atleast": "1080x1920",
				"ratios": "portrait",
				"categories": "100",
				"order": "desc",
				"page": "{page}",
				"purity": "110",
				"sorting": "random"
			},
			"max_page": 1000,
			"method": "get",
			"url": "https://wallhaven.cc/search"
		}
	},
	"真人壁纸[来自wallheaven]": {
		"crawl": {
			"crawl": {
				"method": "get",
				"result": {
					"url": {
						"xpath": "//*[@id='wallpaper']/@src"
					}
				},
				"url": {
					"xpath": "//a[@class='preview']/@href"
				}
			},
			"data": {
				"atleast": "1080x1920",
				"ratios": "portrait",
				"categories": "001",
				"order": "desc",
				"page": "{page}",
				"purity": "110",
				"sorting": "random"
			},
			"max_page": 1000,
			"method": "get",
			"url": "https://wallhaven.cc/search"
		}
	}
}
    """.trimIndent()
}