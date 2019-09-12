package com.problem.dto;

public class Page {
	//page=1&limit=3
	private int page;
	private int limit;//Ïàµ±ÓÚpageSize¡£
	public int getPage() {
		return page;
	}
	public void setPage(int page) {
		this.page = page;
	}
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	@Override
	public String toString() {
		return "Page [page=" + page + ", limit=" + limit + "]";
	}
	
	
	
}
