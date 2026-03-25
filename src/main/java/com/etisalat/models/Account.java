package com.etisalat.models;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "account")
@Data
public class Account {


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true, nullable=false)
    private String accountNo;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User userId;


}
