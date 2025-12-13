package com.group17.lilyoutube_server.util.mappers;

import com.group17.lilyoutube_server.dto.UserDTO;
import com.group17.lilyoutube_server.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDto(User user);

    User toEntity(UserDTO userDTO);
}
