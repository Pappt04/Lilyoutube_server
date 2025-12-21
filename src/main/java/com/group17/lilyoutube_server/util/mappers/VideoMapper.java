package com.group17.lilyoutube_server.util.mappers;

import com.group17.lilyoutube_server.dto.UserDTO;
import com.group17.lilyoutube_server.dto.VideoDTO;
import com.group17.lilyoutube_server.model.User;
import com.group17.lilyoutube_server.model.Video;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VideoMapper {
    VideoDTO toDto(Video video);

    Video toEntity(VideoDTO videoDTO);
}
