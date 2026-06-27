import React from 'react';
import { motion } from 'framer-motion';

interface ToggleProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
  disabled?: boolean;
}

export function Toggle({ checked, onChange, label, disabled = false }: ToggleProps) {
  return (
    <label
      className={`flex items-center gap-2 ${disabled ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer'}`}
    >
      <div
        className={`relative w-9 h-5 rounded-full transition-colors duration-200 ${
          checked ? 'bg-accent-primary' : 'bg-white/12'
        }`}
        onClick={() => !disabled && onChange(!checked)}
      >
        <motion.div
          className="absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow"
          animate={{ x: checked ? 16 : 0 }}
          transition={{ type: 'spring', stiffness: 500, damping: 30 }}
        />
      </div>
      {label && (
        <span className="text-sm text-text-secondary select-none">{label}</span>
      )}
    </label>
  );
}
