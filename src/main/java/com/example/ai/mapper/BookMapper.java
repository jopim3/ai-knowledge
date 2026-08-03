package com.example.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ai.entity.Book;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
}