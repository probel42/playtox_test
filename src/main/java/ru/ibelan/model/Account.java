package ru.ibelan.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "account")
public class Account {
	@Id
	private UUID id;

	@Column
	private Long money;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Long getMoney() {
		return money;
	}

	public void setMoney(Long money) {
		this.money = money;
	}
}
