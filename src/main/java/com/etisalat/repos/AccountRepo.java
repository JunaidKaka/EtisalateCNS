package com.etisalat.repos;

import com.etisalat.models.Account;
import com.etisalat.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepo extends JpaRepository<Account,Long> {

    public List<Account> findAccountByUserId(User user);
}
