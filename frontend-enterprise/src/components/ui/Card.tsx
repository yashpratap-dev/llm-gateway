import React from 'react';
import { motion } from 'framer-motion';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  hover?: boolean;
  onClick?: () => void;
  padding?: 'none' | 'sm' | 'md' | 'lg';
}

const paddingClasses = {
  none: '',
  sm: 'p-3',
  md: 'p-4',
  lg: 'p-6',
};

export function Card({
  children,
  className = '',
  hover = false,
  onClick,
  padding = 'md',
}: CardProps) {
  const Component = hover || onClick ? motion.div : 'div';
  const motionProps =
    hover || onClick
      ? {
          whileHover: { scale: 1.005, boxShadow: '0 0 24px rgba(25,211,255,0.06)' },
          transition: { duration: 0.2 },
        }
      : {};

  return (
    <Component
      onClick={onClick}
      {...motionProps}
      className={`
        bg-bg-card border border-white/8 rounded-xl
        ${paddingClasses[padding]}
        ${onClick ? 'cursor-pointer' : ''}
        ${className}
      `}
    >
      {children}
    </Component>
  );
}
