package com.intelliguard.userservice.userservice.repository;

import com.intelliguard.userservice.userservice.entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
}
